package com.example.bivoca.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.bivoca.data.api.FlashcardApi;
import com.example.bivoca.data.api.RetrofitClient;
import com.example.bivoca.data.api.VocabularyApi;
import com.example.bivoca.data.model.Flashcard;
import com.example.bivoca.data.model.FlashcardPage;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FlashcardRepository {
    private final FlashcardApi api;
    private final VocabularyApi vocabularyApi;

    public FlashcardRepository(Context context) {
        this.api=RetrofitClient.getInstance().create(FlashcardApi.class);
        this.vocabularyApi=RetrofitClient.getInstance().create(VocabularyApi.class);
    }

    /** API-only seam retained for existing contract/ViewModel tests. App uses the Context constructor. */
    public FlashcardRepository(FlashcardApi api) {
        this.api = api;
        this.vocabularyApi = null;
    }

    public interface FlashcardsCallback {
        void onSuccess(List<Flashcard> flashcards);
        void onError(String message);
    }
    public interface FlashcardPageCallback {
        void onSuccess(FlashcardPage page);
        void onError(String message);
    }

    public Call<?> getFlashcardPage(String topicCode, int page, int limit, FlashcardPageCallback callback) {
        if (vocabularyApi == null) {
            return getFlashcards(topicCode, new FlashcardsCallback() {
                @Override public void onSuccess(List<Flashcard> body) {
                    callback.onSuccess(new FlashcardPage(body,1,Math.max(limit,body.size()),body.size(),1));
                }
                @Override public void onError(String message) { callback.onError(message); }
            });
        }
        Call<FlashcardPage> call = vocabularyApi.search(null, topicCode, null, page, limit);
        call.enqueue(new Callback<FlashcardPage>() {
            @Override public void onResponse(@NonNull Call<FlashcardPage> request,
                    @NonNull Response<FlashcardPage> response) {
                if (request.isCanceled()) return;
                FlashcardPage body = response.body();
                if (response.isSuccessful() && body != null && body.getItems() != null
                        && body.getPage() == page && body.getLimit() == limit && hasValidCards(body.getItems())) {
                    callback.onSuccess(body);
                } else {
                    if (response.errorBody() != null) response.errorBody().close();
                    callback.onError("Không thể kết nối đến máy chủ. Vui lòng kiểm tra Internet và thử lại.");
                }
            }
            @Override public void onFailure(@NonNull Call<FlashcardPage> request, @NonNull Throwable failure) {
                if (!request.isCanceled()) callback.onError("Không thể kết nối đến máy chủ. Vui lòng kiểm tra Internet và thử lại.");
            }
        });
        return call;
    }

    public static boolean isValidTopicCode(String code) {
        return code != null && code.matches("^[A-Z][A-Z0-9_]{0,49}$");
    }

    /** Online-only: returns the active API call so obsolete requests can be cancelled. */
    public Call<List<Flashcard>> getFlashcards(String topicCode, FlashcardsCallback callback) {
        Call<List<Flashcard>> call = api.getFlashcards(topicCode);
        call.enqueue(new Callback<List<Flashcard>>() {
            @Override public void onResponse(@NonNull Call<List<Flashcard>> request,
                                             @NonNull Response<List<Flashcard>> response) {
                if (request.isCanceled()) return;
                List<Flashcard> body = response.body();
                if (response.isSuccessful() && body != null && hasValidCards(body)) {
                    callback.onSuccess(body);
                } else {
                    if (response.errorBody() != null) response.errorBody().close();
                    callback.onError("Không thể kết nối đến máy chủ. Vui lòng kiểm tra Internet và thử lại.");
                }
            }
            @Override public void onFailure(@NonNull Call<List<Flashcard>> request,
                                            @NonNull Throwable failure) {
                if (!request.isCanceled()) callback.onError(
                        "Không thể kết nối đến máy chủ. Vui lòng kiểm tra Internet và thử lại.");
            }
        });
        return call;
    }

    private static boolean hasValidCards(List<Flashcard> cards) {
        Set<Integer> ids = new HashSet<>();
        for (Flashcard card : cards) {
            if (card == null || card.getId() <= 0 || card.getEnglish() == null
                    || card.getEnglish().trim().isEmpty() || card.getVietnamese() == null
                    || card.getVietnamese().trim().isEmpty() || !ids.add(card.getId())) return false;
        }
        return true;
    }
}
