package com.example.bivoca.data.repository;

import com.example.bivoca.data.model.QuizSessionDtos;
import android.content.Context;

import androidx.annotation.NonNull;

import com.example.bivoca.data.api.AuthenticatedRetrofitClient;
import com.example.bivoca.data.api.QuizApi;
import com.example.bivoca.data.model.QuizAnswerRequest;
import com.example.bivoca.data.model.QuizAnswerResponse;
import com.example.bivoca.data.model.QuizQuestion;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizRepository {

    private static final String CONNECTION_ERROR =
            "Không thể kết nối đến máy chủ. Vui lòng kiểm tra Internet và thử lại.";
    private static final Pattern AVAILABILITY = Pattern.compile(
            "Only (\\d+) safe quiz question\\(s\\) are available; (\\d+) requested");

    private final QuizApi api;

    public QuizRepository(Context context) {
        retrofit2.Retrofit base = AuthenticatedRetrofitClient.getInstance(context);
        if (com.example.bivoca.BuildConfig.QUIZ_SESSION_V2_ENABLED) {
            okhttp3.OkHttpClient client = ((okhttp3.OkHttpClient) base.callFactory()).newBuilder()
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS).build();
            this.api = base.newBuilder().client(client).build().create(QuizApi.class);
        } else {
            this.api = base.create(QuizApi.class);
        }
    }

    /** Package-private constructor for unit testing with a mock API. */
    QuizRepository(QuizApi api) {
        this.api = api;
    }

    public interface QuestionsCallback {
        void onSuccess(List<QuizQuestion> questions);
        void onError(String message);
    }

    public interface AnswerCallback {
        void onSuccess(QuizAnswerResponse response);
        void onError(String message);
    }

    /**
     * Tải câu hỏi quiz cho một chủ đề.
     * @param topicCode null = lấy ngẫu nhiên toàn bộ chủ đề
     * @param limit     số lượng câu hỏi (1-20)
     * @return Call để ViewModel có thể cancel khi cần
     */
    public Call<List<QuizQuestion>> loadQuestions(
            String topicCode,
            int limit,
            QuestionsCallback callback
    ) {
        Call<List<QuizQuestion>> call = api.getQuestions(topicCode, limit);
        call.enqueue(new Callback<List<QuizQuestion>>() {
            @Override
            public void onResponse(@NonNull Call<List<QuizQuestion>> request,
                                   @NonNull Response<List<QuizQuestion>> response) {
                if (request.isCanceled()) return;
                if (response.isSuccessful()) {
                    List<QuizQuestion> body = response.body();
                    if (body == null || body.isEmpty()) {
                        callback.onError("Không đủ câu hỏi cho chủ đề này. Hãy thử chủ đề khác.");
                    } else {
                        callback.onSuccess(body);
                    }
                    return;
                }
                String serverMessage = readErrorMessage(response);
                switch (response.code()) {
                    case 401:
                        callback.onError("Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.");
                        break;
                    case 404:
                        callback.onError("Không tìm thấy chủ đề này.");
                        break;
                    case 400:
                        Matcher availability = serverMessage == null ? null : AVAILABILITY.matcher(serverMessage);
                        if (availability != null && availability.matches()) {
                            callback.onError("Chủ đề này hiện có " + availability.group(1)
                                    + " câu hỏi an toàn, chưa đủ " + availability.group(2) + " câu.");
                        } else {
                            callback.onError("Không đủ câu hỏi an toàn cho chủ đề này. Hãy thử chủ đề khác.");
                        }
                        break;
                    default:
                        callback.onError("Không tải được câu hỏi. Hãy thử lại (HTTP " + response.code() + ").");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<QuizQuestion>> request, @NonNull Throwable t) {
                if (request.isCanceled()) return;
                if (t instanceof SocketTimeoutException) {
                    callback.onError("Máy chủ phản hồi quá lâu. Hãy thử lại.");
                } else if (t instanceof IOException) {
                    callback.onError(CONNECTION_ERROR);
                } else {
                    callback.onError("Không thể tải câu hỏi. Hãy thử lại.");
                }
            }
        });
        return call;
    }

    /**
     * Gửi đáp án cho một câu hỏi.
     * @return Call để ViewModel có thể cancel khi cần
     */
    public Call<QuizAnswerResponse> submitAnswer(
            int flashcardId,
            int selectedFlashcardId,
            AnswerCallback callback
    ) {
        Call<QuizAnswerResponse> call = api.submitAnswer(
                new QuizAnswerRequest(flashcardId, selectedFlashcardId));
        call.enqueue(new Callback<QuizAnswerResponse>() {
            @Override
            public void onResponse(@NonNull Call<QuizAnswerResponse> request,
                                   @NonNull Response<QuizAnswerResponse> response) {
                if (request.isCanceled()) return;
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }
                if (response.errorBody() != null) response.errorBody().close();
                if (response.code() == 401) {
                    callback.onError("Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.");
                } else {
                    callback.onError("Không gửi được đáp án. Hãy thử lại (HTTP " + response.code() + ").");
                }
            }

            @Override
            public void onFailure(@NonNull Call<QuizAnswerResponse> request, @NonNull Throwable t) {
                if (request.isCanceled()) return;
                if (t instanceof IOException) {
                    callback.onError(CONNECTION_ERROR);
                } else {
                    callback.onError("Không thể gửi đáp án. Hãy thử lại.");
                }
            }
        });
        return call;
    }

    private static String readErrorMessage(Response<?> response) {
        if (response.errorBody() == null) return null;
        try {
            JsonObject body = JsonParser.parseString(response.errorBody().string()).getAsJsonObject();
            return body.getAsJsonObject("error").get("message").getAsString();
        } catch (Exception ignored) {
            response.errorBody().close();
            return null;
        }
    }

    public interface V2Callback<T> { void onSuccess(T value); void onError(String code,String message); }
    private <T> Call<T> enqueueV2(Call<T> call,V2Callback<T> callback) {
        call.enqueue(new Callback<T>() {
            @Override public void onResponse(@NonNull Call<T> req,@NonNull Response<T> res) {
                if(req.isCanceled())return;
                if(res.isSuccessful()&&res.body()!=null){callback.onSuccess(res.body());return;}
                String code="HTTP_"+res.code();
                if(res.errorBody()!=null)try(okhttp3.ResponseBody body=res.errorBody()){
                    JsonObject error=JsonParser.parseString(body.string()).getAsJsonObject().getAsJsonObject("error");
                    if(error!=null&&error.has("code"))code=error.get("code").getAsString();
                }catch(Exception ignored){ }
                String message=res.code()==401?"Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.":
                        ("AVAILABILITY_CHANGED".equals(code)?"Số câu khả dụng đã thay đổi. Vui lòng làm mới cấu hình.":
                        (res.code()==409?"Phiên đã thay đổi. Hãy tải lại phiên để đối chiếu kết quả.":"Không hoàn tất yêu cầu (HTTP "+res.code()+"). Hãy thử lại."));
                callback.onError(code,message);
            }
            @Override public void onFailure(@NonNull Call<T> req,@NonNull Throwable failure) {
                if(!req.isCanceled())callback.onError("NETWORK",failure instanceof SocketTimeoutException?"Máy chủ phản hồi quá lâu. Đáp án đang chờ xác nhận; thử lại để lấy đúng kết quả đã lưu.":CONNECTION_ERROR);
            }
        });return call;
    }
    public Call<QuizSessionDtos.Availability> availability(String method,String topic,String pos,V2Callback<QuizSessionDtos.Availability> callback) { return enqueueV2(api.availability(method,topic,pos),callback); }
    public Call<QuizSessionDtos.Creation> createSession(QuizSessionDtos.Create input,V2Callback<QuizSessionDtos.Creation> callback) { return enqueueV2(api.createSession(input),callback); }
    public Call<QuizSessionDtos.Session> resumeSession(String id,V2Callback<QuizSessionDtos.Session> callback) { return enqueueV2(api.resumeSession(id),callback); }
    public Call<List<QuizSessionDtos.Question>> sessionQuestions(String id,V2Callback<List<QuizSessionDtos.Question>> callback) { return enqueueV2(api.sessionQuestions(id,0,100),callback); }
    public Call<QuizSessionDtos.Receipt> answerSession(String id,QuizSessionDtos.Answer input,V2Callback<QuizSessionDtos.Receipt> callback) { return enqueueV2(api.answerSession(id,input),callback); }
    public Call<QuizSessionDtos.Receipt> controlSession(String id,boolean abandon,QuizSessionDtos.Control input,V2Callback<QuizSessionDtos.Receipt> callback) { return enqueueV2(abandon?api.abandonSession(id,input):api.completeSession(id,input),callback); }

}
