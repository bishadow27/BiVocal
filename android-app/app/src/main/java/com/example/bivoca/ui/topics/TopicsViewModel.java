package com.example.bivoca.ui.topics;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bivoca.data.model.Topic;
import com.example.bivoca.data.repository.TopicRepository;

import java.util.List;

import retrofit2.Call;

public class TopicsViewModel extends ViewModel {

    private final TopicRepository repository;

    private final MutableLiveData<List<Topic>> topics =
            new MutableLiveData<>();

    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(false);

    private final MutableLiveData<String> error =
            new MutableLiveData<>();

    private Call<List<Topic>> activeCall;
    private int requestVersion;
    private boolean requested;

    public TopicsViewModel(TopicRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<Topic>> getTopics() {
        return topics;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    /** An Activity recreation reuses both the result and any in-flight request. */
    public void ensureTopicsLoaded() {
        if (!requested) loadTopics();
    }

    public void loadTopics() {
        requested = true;
        int version = ++requestVersion;
        if (activeCall != null) activeCall.cancel();
        loading.setValue(true);
        error.setValue(null);
        topics.setValue(null);
        activeCall = repository.getTopics(
                new TopicRepository.TopicsCallback() {

                    @Override
                    public void onSuccess(List<Topic> result) {
                        if (version != requestVersion) return;
                        topics.setValue(result);
                        loading.setValue(false);
                    }

                    @Override
                    public void onError(
                            String message
                    ) {
                        if (version != requestVersion) return;
                        error.setValue(message);
                        loading.setValue(false);
                    }
                }
        );
    }

    @Override
    protected void onCleared() {
        requestVersion++;
        if (activeCall != null) activeCall.cancel();
    }
}
