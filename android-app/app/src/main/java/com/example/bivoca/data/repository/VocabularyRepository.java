package com.example.bivoca.data.repository;

import androidx.annotation.NonNull;

import com.example.bivoca.data.api.RetrofitClient;
import com.example.bivoca.data.api.VocabularyApi;
import com.example.bivoca.data.model.FlashcardPage;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class VocabularyRepository {
    private final VocabularyApi api;

    public VocabularyRepository() {
        this(RetrofitClient.getInstance().create(VocabularyApi.class));
    }

    VocabularyRepository(VocabularyApi api) { this.api = api; }

    public interface SearchCallback {
        void onSuccess(FlashcardPage page);
        void onError(String message);
    }

    public Call<FlashcardPage> search(String query, String topicCode, String level, int page, SearchCallback callback) {
        // Retrofit omits null query parameters. Sending search= makes the backend's
        // min(1) validation reject topic-only paging with HTTP 400.
        String search = query == null || query.trim().isEmpty() ? null : query.trim();
        Call<FlashcardPage> call = api.search(search, topicCode, level, page, 20);
        call.enqueue(new Callback<FlashcardPage>() {
            @Override public void onResponse(@NonNull Call<FlashcardPage> request,
                                             @NonNull Response<FlashcardPage> response) {
                if (request.isCanceled()) return;
                FlashcardPage body = response.body();
                if (response.isSuccessful() && body != null && body.getItems() != null
                        && body.getPage() >= 1 && body.getLimit() == 20 && body.getTotal() >= 0) {
                    callback.onSuccess(body);
                } else {
                    if (response.errorBody() != null) response.errorBody().close();
                    callback.onError("Không thể tìm từ vựng. Hãy thử lại.");
                }
            }

            @Override public void onFailure(@NonNull Call<FlashcardPage> request,
                                            @NonNull Throwable failure) {
                if (!request.isCanceled()) callback.onError(
                        "Không thể kết nối đến máy chủ. Vui lòng kiểm tra Internet và thử lại.");
            }
        });
        return call;
    }
}
