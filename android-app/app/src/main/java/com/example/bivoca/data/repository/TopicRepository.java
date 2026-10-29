package com.example.bivoca.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.bivoca.data.api.RetrofitClient;
import com.example.bivoca.data.api.TopicApi;
import com.example.bivoca.data.model.Topic;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TopicRepository {
    private final TopicApi api;

    public TopicRepository(Context context) {
        this(RetrofitClient.getInstance().create(TopicApi.class));
    }

    public TopicRepository(TopicApi api) {
        this.api = api;
    }

    public interface TopicsCallback {
        void onSuccess(List<Topic> topics);
        void onError(String message);
    }

    /** Online-only: an API failure is surfaced and is never replaced with stale local data. */
    public Call<List<Topic>> getTopics(TopicsCallback callback) {
        Call<List<Topic>> call = api.getTopics();
        call.enqueue(new Callback<List<Topic>>() {
            @Override public void onResponse(@NonNull Call<List<Topic>> request,
                                             @NonNull Response<List<Topic>> response) {
                if (request.isCanceled()) return;
                List<Topic> body = response.body();
                if (response.isSuccessful() && body != null && hasValidTopics(body)) {
                    callback.onSuccess(body);
                } else {
                    if (response.errorBody() != null) response.errorBody().close();
                    callback.onError("Không thể kết nối đến máy chủ. Vui lòng kiểm tra Internet và thử lại.");
                }
            }
            @Override public void onFailure(@NonNull Call<List<Topic>> request,
                                            @NonNull Throwable failure) {
                if (!request.isCanceled()) callback.onError(
                        "Không thể kết nối đến máy chủ. Vui lòng kiểm tra Internet và thử lại.");
            }
        });
        return call;
    }

    private static boolean hasValidTopics(List<Topic> topics) {
        Set<Integer> ids = new HashSet<>();
        Set<String> codes = new HashSet<>();
        for (Topic topic : topics) {
            if (topic == null || topic.getId() <= 0
                    || !FlashcardRepository.isValidTopicCode(topic.getCode())
                    || topic.getName() == null || topic.getName().trim().isEmpty()
                    || !ids.add(topic.getId()) || !codes.add(topic.getCode())) return false;
        }
        return true;
    }
}
