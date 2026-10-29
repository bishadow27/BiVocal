package com.example.bivoca.ui.search;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bivoca.data.model.FlashcardPage;
import com.example.bivoca.data.repository.VocabularyRepository;

import retrofit2.Call;

public final class VocabularySearchViewModel extends ViewModel {
    private final VocabularyRepository repository;
    private final MutableLiveData<FlashcardPage> result = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private Call<FlashcardPage> call;
    private String query = "";
    private String level;
    private String topicCode;

    public VocabularySearchViewModel(VocabularyRepository repository) { this.repository = repository; }
    public LiveData<FlashcardPage> getResult() { return result; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    public void setTopicCode(String value) { topicCode = value == null || value.trim().isEmpty() ? null : value.trim(); }

    public void search(String value, String selectedLevel, int page) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() && topicCode == null) { error.setValue("Hãy nhập từ tiếng Anh hoặc nghĩa tiếng Việt."); return; }
        if (call != null) call.cancel();
        query = normalized; level = selectedLevel;
        loading.setValue(true); error.setValue(null);
        call = repository.search(query, topicCode, level, page, new VocabularyRepository.SearchCallback() {
            @Override public void onSuccess(FlashcardPage pageResult) {
                loading.setValue(false); result.setValue(pageResult);
            }
            @Override public void onError(String message) {
                loading.setValue(false); error.setValue(message);
            }
        });
    }

    public void nextPage() {
        FlashcardPage current = result.getValue();
        if (current != null && current.getPage() < current.getTotalPages()) search(query, level, current.getPage() + 1);
    }

    @Override protected void onCleared() { if (call != null) call.cancel(); }
}
