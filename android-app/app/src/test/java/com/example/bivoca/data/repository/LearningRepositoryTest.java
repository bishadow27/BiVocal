package com.example.bivoca.data.repository;

import com.example.bivoca.data.api.FlashcardApi;
import com.example.bivoca.data.api.TopicApi;
import com.example.bivoca.data.model.Flashcard;
import com.example.bivoca.data.model.Topic;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.Assert.*;

/** Contract tests for the Online-Only learning repositories. */
public class LearningRepositoryTest {
    private static final String CONNECTION_ERROR =
            "Không thể kết nối đến máy chủ. Vui lòng kiểm tra Internet và thử lại.";
    private final List<Topic> topics = new Gson().fromJson(
            "[{\"id\":1,\"code\":\"FOOD\",\"name\":\"Food\"}]",
            new TypeToken<List<Topic>>() {}.getType());
    private final List<Flashcard> cards = new Gson().fromJson(
            "[{\"id\":1,\"english\":\"apple\",\"vietnamese\":\"quả táo\"}]",
            new TypeToken<List<Flashcard>>() {}.getType());

    @Test public void topicRepositoryReturnsOnlyApiResponse() {
        PendingCall<List<Topic>> pending = new PendingCall<>();
        TopicApi api = () -> pending;
        Result<Topic> result = new Result<>();
        Call<List<Topic>> returned = new TopicRepository(api).getTopics(result);
        assertSame(pending, returned);
        pending.respond(Response.success(topics));
        assertEquals(topics, result.values);
        assertNull(result.error);
    }

    @Test public void topicNetworkFailureIsVisibleAndHasNoLocalFallback() {
        PendingCall<List<Topic>> pending = new PendingCall<>();
        Result<Topic> result = new Result<>();
        new TopicRepository(() -> pending).getTopics(result);
        pending.fail(new IOException("offline"));
        assertNull(result.values);
        assertEquals(CONNECTION_ERROR, result.error);
    }

    @Test public void flashcardRepositoryReturnsOnlyApiResponse() {
        PendingCall<List<Flashcard>> pending = new PendingCall<>();
        FlashcardApi api = code -> pending;
        Result<Flashcard> result = new Result<>();
        Call<List<Flashcard>> returned = new FlashcardRepository(api).getFlashcards("FOOD", result);
        assertSame(pending, returned);
        pending.respond(Response.success(cards));
        assertEquals(cards, result.values);
        assertNull(result.error);
    }

    @Test public void flashcardNetworkFailureIsVisibleAndHasNoLocalFallback() {
        PendingCall<List<Flashcard>> pending = new PendingCall<>();
        Result<Flashcard> result = new Result<>();
        new FlashcardRepository(code -> pending).getFlashcards("FOOD", result);
        pending.fail(new IOException("offline"));
        assertNull(result.values);
        assertEquals(CONNECTION_ERROR, result.error);
    }

    private static final class Result<T> implements TopicRepository.TopicsCallback,
            FlashcardRepository.FlashcardsCallback {
        List<T> values;
        String error;
        @SuppressWarnings("unchecked")
        @Override public void onSuccess(List values) { values = List.copyOf(values); this.values = values; }
        @Override public void onError(String message) { error = message; }
    }

    private static final class PendingCall<T> implements Call<T> {
        private Callback<T> callback;
        private boolean canceled;
        @Override public void enqueue(Callback<T> callback) { this.callback = callback; }
        void respond(Response<T> response) { callback.onResponse(this, response); }
        void fail(Throwable error) { callback.onFailure(this, error); }
        @Override public void cancel() { canceled = true; }
        @Override public boolean isCanceled() { return canceled; }
        @Override public boolean isExecuted() { return callback != null; }
        @Override public Response<T> execute() { throw new UnsupportedOperationException(); }
        @Override public Call<T> clone() { return new PendingCall<>(); }
        @Override public Request request() { return new Request.Builder().url("http://localhost/").build(); }
        @Override public Timeout timeout() { return Timeout.NONE; }
    }
}
