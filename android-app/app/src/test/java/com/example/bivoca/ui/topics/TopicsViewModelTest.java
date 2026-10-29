package com.example.bivoca.ui.topics;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;

import com.example.bivoca.data.model.Topic;
import com.example.bivoca.data.repository.TopicRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.Assert.*;

public class TopicsViewModelTest {
    private final List<Topic> topics = new Gson().fromJson(
            "[{\"id\":1,\"code\":\"FOOD\",\"name\":\"Food\",\"description\":null,\"imageUrl\":null}]",
            new TypeToken<List<Topic>>() {}.getType());
    private PendingRepository repository;
    private TopicsViewModel model;

    @Before public void setUp() {
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override public void executeOnDiskIO(Runnable runnable) { runnable.run(); }
            @Override public void postToMainThread(Runnable runnable) { runnable.run(); }
            @Override public boolean isMainThread() { return true; }
        });
        repository = new PendingRepository();
        model = new TopicsViewModel(repository);
    }

    @After public void tearDown() {
        model.onCleared();
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test public void recreationDoesNotDuplicateInFlightOrCachedLoad() {
        model.ensureTopicsLoaded();
        model.ensureTopicsLoaded();
        assertEquals(1, repository.requests.size());
        repository.requests.get(0).callback.onSuccess(topics);
        model.ensureTopicsLoaded();
        assertEquals(1, repository.requests.size());
        assertEquals(topics, model.getTopics().getValue());
        assertFalse(model.getLoading().getValue());
    }

    @Test public void explicitRetryReplacesCacheWithApiData() {
        model.loadTopics();
        repository.requests.get(0).callback.onSuccess(topics);
        model.loadTopics();
        assertTrue(repository.requests.get(0).call.isCanceled());
        assertTrue(model.getLoading().getValue());
        repository.requests.get(1).callback.onSuccess(Collections.emptyList());
        assertTrue(model.getTopics().getValue().isEmpty());
        assertNull(model.getError().getValue());
    }

    @Test public void obsoleteCacheReadAndFailureCannotReplaceLatestResult() {
        model.loadTopics();
        PendingRequest first = repository.requests.get(0);
        model.loadTopics();
        repository.requests.get(1).callback.onSuccess(Collections.emptyList());
        first.callback.onSuccess(topics);
        first.callback.onError("old failure");
        assertTrue(model.getTopics().getValue().isEmpty());
        assertNull(model.getError().getValue());
    }

    @Test public void missingCacheShowsErrorUntilExplicitRetry() {
        model.ensureTopicsLoaded();
        repository.requests.get(0).callback.onError("Chưa có dữ liệu đã lưu.");
        model.ensureTopicsLoaded();
        assertEquals(1, repository.requests.size());
        assertFalse(model.getLoading().getValue());
        assertNull(model.getTopics().getValue());
        assertNotNull(model.getError().getValue());
        model.loadTopics();
        assertNull(model.getError().getValue());
        repository.requests.get(1).callback.onSuccess(topics);
        assertEquals(topics, model.getTopics().getValue());
    }

    @Test public void cachedEmptyListAndClearedViewModelAreSafe() {
        model.loadTopics();
        repository.requests.get(0).callback.onSuccess(Collections.emptyList());
        assertTrue(model.getTopics().getValue().isEmpty());
        assertNull(model.getError().getValue());
        model.loadTopics();
        PendingRequest pending = repository.requests.get(1);
        model.onCleared();
        assertTrue(pending.call.isCanceled());
        pending.callback.onSuccess(topics);
        assertNull(model.getTopics().getValue());
    }

    private static class PendingRepository extends TopicRepository {
        final List<PendingRequest> requests = new ArrayList<>();
        PendingRepository() { super(() -> null); }
        @Override public Call<List<Topic>> getTopics(TopicsCallback callback) {
            PendingRequest pending = new PendingRequest(callback);
            requests.add(pending);
            return pending.call;
        }
    }

    private static class PendingRequest {
        final TopicRepository.TopicsCallback callback;
        final PendingCall call = new PendingCall();
        PendingRequest(TopicRepository.TopicsCallback callback) { this.callback = callback; }
    }

    private static class PendingCall implements Call<List<Topic>> {
        private boolean canceled;
        @Override public void cancel() { canceled = true; }
        @Override public boolean isCanceled() { return canceled; }
        @Override public boolean isExecuted() { return false; }
        @Override public Response<List<Topic>> execute() { throw new UnsupportedOperationException(); }
        @Override public void enqueue(Callback<List<Topic>> callback) { throw new UnsupportedOperationException(); }
        @Override public Call<List<Topic>> clone() { return new PendingCall(); }
        @Override public Request request() { return new Request.Builder().url("http://localhost/").build(); }
        @Override public Timeout timeout() { return Timeout.NONE; }
    }
}
