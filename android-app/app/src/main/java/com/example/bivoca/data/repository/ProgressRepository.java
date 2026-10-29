package com.example.bivoca.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.bivoca.data.api.AuthenticatedRetrofitClient;
import com.example.bivoca.data.api.ProgressApi;
import com.example.bivoca.data.model.FavoriteRequest;
import com.example.bivoca.data.model.ProgressDto;
import com.example.bivoca.data.model.ProgressSummary;
import com.example.bivoca.data.model.ReviewRequest;
import com.example.bivoca.data.model.ReviewSchedule;
import com.example.bivoca.data.model.Flashcard;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProgressRepository {

    private final ProgressApi api;

    public ProgressRepository(Context context) {
        this.api = AuthenticatedRetrofitClient.getInstance(context).create(ProgressApi.class);
    }

    ProgressRepository(ProgressApi api) {
        this.api = api;
    }

    /** For repository doubles in local unit tests only. */
    protected ProgressRepository() { this.api = null; }

    public interface SummaryCallback {
        void onSuccess(ProgressSummary summary);
        void onError(String message);
    }

    public interface ProgressCallback {
        void onSuccess(ProgressDto progress);
        void onError(String message);
    }

    public interface ScheduleCallback {
        void onSuccess(ReviewSchedule schedule);
        void onError(String message);
    }

    public interface DueCardsCallback {
        void onSuccess(List<Flashcard> cards);
        void onError(String message);
    }

    public Call<ReviewSchedule> getSchedule(ScheduleCallback callback) {
        Call<ReviewSchedule> call = api.getSchedule();
        call.enqueue(new Callback<ReviewSchedule>() {
            @Override public void onResponse(@NonNull Call<ReviewSchedule> request,
                                             @NonNull Response<ReviewSchedule> response) {
                if (request.isCanceled()) return;
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body());
                else {
                    if (response.errorBody() != null) response.errorBody().close();
                    callback.onError("Không tải được lịch ôn tập (HTTP " + response.code() + ").");
                }
            }
            @Override public void onFailure(@NonNull Call<ReviewSchedule> request, @NonNull Throwable t) {
                if (!request.isCanceled()) callback.onError("Không kết nối được máy chủ để tải lịch ôn tập.");
            }
        });
        return call;
    }

    public Call<List<Flashcard>> getDueCards(DueCardsCallback callback) {
        Call<List<Flashcard>> call = api.getDueCards(300);
        call.enqueue(new Callback<List<Flashcard>>() {
            @Override public void onResponse(@NonNull Call<List<Flashcard>> request,
                                             @NonNull Response<List<Flashcard>> response) {
                if (request.isCanceled()) return;
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body());
                else {
                    if (response.errorBody() != null) response.errorBody().close();
                    callback.onError("Không tải được từ đến hạn (HTTP " + response.code() + ").");
                }
            }
            @Override public void onFailure(@NonNull Call<List<Flashcard>> request, @NonNull Throwable t) {
                if (!request.isCanceled()) callback.onError("Cần kết nối máy chủ để xem từ đến hạn.");
            }
        });
        return call;
    }

    public Call<ProgressSummary> getSummary(SummaryCallback callback) {
        Call<ProgressSummary> call = api.getSummary();
        call.enqueue(new Callback<ProgressSummary>() {
            @Override
            public void onResponse(@NonNull Call<ProgressSummary> request, @NonNull Response<ProgressSummary> response) {
                if (request.isCanceled()) return;
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }
                if (response.errorBody() != null) response.errorBody().close();
                callback.onError("Không tải được thống kê tiến độ (HTTP " + response.code() + ").");
            }

            @Override
            public void onFailure(@NonNull Call<ProgressSummary> request, @NonNull Throwable t) {
                if (request.isCanceled()) return;
                callback.onError(t instanceof IOException ? "Không kết nối được máy chủ." : "Lỗi tải dữ liệu tiến độ.");
            }
        });
        return call;
    }

    public Call<ProgressDto> getProgress(int flashcardId, ProgressCallback callback) {
        Call<ProgressDto> call = api.getProgress(flashcardId);
        call.enqueue(new Callback<ProgressDto>() {
            @Override
            public void onResponse(@NonNull Call<ProgressDto> request, @NonNull Response<ProgressDto> response) {
                if (request.isCanceled()) return;
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }
                if (response.errorBody() != null) response.errorBody().close();
                callback.onError("Không tải được tiến độ từ vựng.");
            }

            @Override
            public void onFailure(@NonNull Call<ProgressDto> request, @NonNull Throwable t) {
                if (request.isCanceled()) return;
                callback.onError("Lỗi kết nối máy chủ.");
            }
        });
        return call;
    }

    public Call<ProgressDto> submitReview(int flashcardId, boolean correct, ProgressCallback callback) {
        Call<ProgressDto> call = api.submitReview(flashcardId, new ReviewRequest(correct));
        call.enqueue(new Callback<ProgressDto>() {
            @Override
            public void onResponse(@NonNull Call<ProgressDto> request, @NonNull Response<ProgressDto> response) {
                if (request.isCanceled()) return;
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }
                if (response.errorBody() != null) response.errorBody().close();
                callback.onError("Không lưu được kết quả ôn tập.");
            }

            @Override
            public void onFailure(@NonNull Call<ProgressDto> request, @NonNull Throwable t) {
                if (request.isCanceled()) return;
                callback.onError("Lỗi kết nối khi lưu kết quả.");
            }
        });
        return call;
    }

    public Call<ProgressDto> toggleFavorite(int flashcardId, boolean isFavorite, ProgressCallback callback) {
        Call<ProgressDto> call = api.setFavorite(flashcardId, new FavoriteRequest(isFavorite));
        call.enqueue(new Callback<ProgressDto>() {
            @Override
            public void onResponse(@NonNull Call<ProgressDto> request, @NonNull Response<ProgressDto> response) {
                if (request.isCanceled()) return;
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }
                if (response.errorBody() != null) response.errorBody().close();
                callback.onError("Không cập nhật được từ yêu thích.");
            }

            @Override
            public void onFailure(@NonNull Call<ProgressDto> request, @NonNull Throwable t) {
                if (request.isCanceled()) return;
                callback.onError("Lỗi kết nối máy chủ.");
            }
        });
        return call;
    }
}
