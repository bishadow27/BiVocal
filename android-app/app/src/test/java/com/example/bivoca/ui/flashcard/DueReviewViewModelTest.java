package com.example.bivoca.ui.flashcard;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;
import androidx.lifecycle.SavedStateHandle;

import com.example.bivoca.data.model.Flashcard;
import com.example.bivoca.data.model.ProgressDto;
import com.example.bivoca.data.repository.FlashcardRepository;
import com.example.bivoca.data.repository.ProgressRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.Assert.*;

public class DueReviewViewModelTest {
    private final List<Flashcard> cards = new Gson().fromJson("["
            + "{\"id\":1,\"english\":\"apple\",\"vietnamese\":\"táo\",\"memoryLevel\":2},"
            + "{\"id\":2,\"english\":\"bread\",\"vietnamese\":\"bánh mì\",\"memoryLevel\":0}]",
            new TypeToken<List<Flashcard>>() {}.getType());
    private final Gson gson = new Gson();
    private PendingProgress progress;
    private FlashcardViewModel model;
    private int publicRequests;

    @Before public void setUp() {
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override public void executeOnDiskIO(Runnable runnable) { runnable.run(); }
            @Override public void postToMainThread(Runnable runnable) { runnable.run(); }
            @Override public boolean isMainThread() { return true; }
        });
        progress = new PendingProgress();
        FlashcardRepository publicRepository = new FlashcardRepository(code -> null) {
            @Override public Call<List<Flashcard>> getFlashcards(String topic, FlashcardsCallback callback) {
                publicRequests++;
                throw new AssertionError("Due mode must not use public Room-backed list");
            }
        };
        model = new FlashcardViewModel(new SavedStateHandle(), publicRepository, progress);
    }

    @After public void tearDown() {
        model.onCleared();
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test public void loadingEmptyRetryAndOldResponse() {
        model.ensureDueLoaded();
        model.ensureDueLoaded(); // Activity recreation reuses the ViewModel.
        assertEquals(1, progress.loads.size());
        PendingLoad old = progress.loads.get(0);
        old.callback.onError("Cần kết nối máy chủ");
        assertFalse(model.getLoading().getValue());
        assertNotNull(model.getError().getValue());
        model.loadDueCards();
        assertTrue(old.call.isCanceled());
        old.callback.onSuccess(cards); // obsolete callback must be ignored.
        assertNull(model.getFlashcards().getValue());
        progress.loads.get(1).callback.onSuccess(new ArrayList<>());
        assertTrue(model.getFlashcards().getValue().isEmpty());
        assertFalse(model.getReviewCompleted().getValue());
        assertEquals(Integer.valueOf(0), model.getSessionTotal().getValue());
        assertEquals(Integer.valueOf(0), model.getSessionProcessed().getValue());
        assertEquals(0, publicRequests);
    }

    @Test public void reviewWaitsForBackendAndFailureRetainsCard() {
        model.ensureDueLoaded();
        progress.loads.get(0).callback.onSuccess(cards);
        assertEquals(Integer.valueOf(2), model.getSessionTotal().getValue());
        assertEquals(Integer.valueOf(0), model.getSessionProcessed().getValue());
        model.submitDueReview(false);
        model.submitDueReview(false);
        assertEquals(1, progress.reviews.size());
        assertEquals(1, model.getFlashcards().getValue().get(0).getId());
        assertTrue(model.getReviewPending().getValue());
        progress.reviews.get(0).callback.onError("Lỗi kết nối khi lưu kết quả");
        assertFalse(model.getReviewPending().getValue());
        assertEquals(1, model.getFlashcards().getValue().get(0).getId());
        assertNotNull(model.getReviewError().getValue());
        assertEquals(Integer.valueOf(0), model.getSessionProcessed().getValue());
        model.submitDueReview(true);
        assertNull(model.getReviewError().getValue());
        progress.reviews.get(1).callback.onSuccess(dto(1));
        assertEquals(2, model.getFlashcards().getValue().get(0).getId());
        assertEquals(Integer.valueOf(1), model.getSessionProcessed().getValue());
        assertFalse(model.getReviewCompleted().getValue());
        progress.reviews.get(0).callback.onSuccess(dto(1)); // late failure request cannot advance.
        assertEquals(2, model.getFlashcards().getValue().get(0).getId());
        model.submitDueReview(true);
        progress.reviews.get(2).callback.onSuccess(dto(2));
        assertTrue(model.getFlashcards().getValue().isEmpty());
        assertTrue(model.getReviewCompleted().getValue());
        assertEquals(Integer.valueOf(2), model.getSessionProcessed().getValue());
        assertEquals(Integer.valueOf(2), model.getSessionTotal().getValue());
        model.loadDueCards();
        progress.loads.get(1).callback.onSuccess(new ArrayList<>());
        assertFalse(model.getReviewCompleted().getValue());
        assertEquals(Integer.valueOf(0), model.getSessionTotal().getValue());
        assertEquals(Integer.valueOf(0), model.getSessionProcessed().getValue());
    }

    @Test public void reloadingAfterRotationDoesNotDuplicatePendingReview() {
        model.ensureDueLoaded();
        progress.loads.get(0).callback.onSuccess(cards);
        model.submitDueReview(true);
        model.ensureDueLoaded();
        model.submitDueReview(true);
        assertEquals(1, progress.loads.size());
        assertEquals(1, progress.reviews.size());
        progress.reviews.get(0).callback.onSuccess(dto(1));
        assertEquals(2, model.getFlashcards().getValue().get(0).getId());
    }

    @Test public void clearedModelCancelsOldRequests() {
        model.ensureDueLoaded();
        progress.loads.get(0).callback.onSuccess(cards);
        model.submitDueReview(true);
        model.onCleared();
        assertTrue(progress.reviews.get(0).call.isCanceled());
        progress.reviews.get(0).callback.onSuccess(dto(1));
        assertEquals(1, model.getFlashcards().getValue().get(0).getId());
    }

    @Test public void negativeRatingAlsoCountsOnlyAfterAcknowledgement() {
        model.ensureDueLoaded();
        progress.loads.get(0).callback.onSuccess(cards);
        model.submitDueReview(false);
        assertFalse(progress.reviews.get(0).correct);
        assertEquals(Integer.valueOf(0), model.getSessionProcessed().getValue());
        model.loadDueCards(); // Reload cannot reset a pending session.
        assertEquals(1, progress.loads.size());
        progress.reviews.get(0).callback.onSuccess(dto(1));
        assertEquals(Integer.valueOf(1), model.getSessionProcessed().getValue());
        model.ensureDueLoaded(); // Recreation keeps the same counters and remaining queue.
        assertEquals(Integer.valueOf(2), model.getSessionTotal().getValue());
        assertEquals(Integer.valueOf(1), model.getSessionProcessed().getValue());
        assertEquals(1, model.getFlashcards().getValue().size());
    }

    private ProgressDto dto(int id) {
        return gson.fromJson("{\"flashcardId\":" + id + ",\"memoryLevel\":1}", ProgressDto.class);
    }

    private static class PendingProgress extends ProgressRepository {
        final List<PendingLoad> loads = new ArrayList<>();
        final List<PendingReview> reviews = new ArrayList<>();

        @Override public Call<List<Flashcard>> getDueCards(DueCardsCallback callback) {
            PendingLoad pending = new PendingLoad(callback);
            loads.add(pending);
            return pending.call;
        }

        @Override public Call<ProgressDto> submitReview(int flashcardId, boolean correct, ProgressCallback callback) {
            PendingReview pending = new PendingReview(flashcardId, correct, callback);
            reviews.add(pending);
            return pending.call;
        }
    }

    private static class PendingLoad {
        final ProgressRepository.DueCardsCallback callback;
        final PendingCall<List<Flashcard>> call = new PendingCall<>();
        PendingLoad(ProgressRepository.DueCardsCallback callback) { this.callback = callback; }
    }

    private static class PendingReview {
        final int id;
        final boolean correct;
        final ProgressRepository.ProgressCallback callback;
        final PendingCall<ProgressDto> call = new PendingCall<>();
        PendingReview(int id, boolean correct, ProgressRepository.ProgressCallback callback) {
            this.id = id; this.correct = correct; this.callback = callback;
        }
    }

    private static class PendingCall<T> implements Call<T> {
        private boolean canceled;
        @Override public void cancel() { canceled = true; }
        @Override public boolean isCanceled() { return canceled; }
        @Override public boolean isExecuted() { return false; }
        @Override public Response<T> execute() { throw new UnsupportedOperationException(); }
        @Override public void enqueue(Callback<T> callback) { throw new UnsupportedOperationException(); }
        @Override public Call<T> clone() { return new PendingCall<>(); }
        @Override public Request request() { return new Request.Builder().url("http://localhost/").build(); }
        @Override public Timeout timeout() { return Timeout.NONE; }
    }
}
