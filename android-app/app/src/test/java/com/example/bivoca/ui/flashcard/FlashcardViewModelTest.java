package com.example.bivoca.ui.flashcard;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;
import androidx.lifecycle.SavedStateHandle;

import com.example.bivoca.data.model.Flashcard;
import com.example.bivoca.data.model.FlashcardPage;
import com.example.bivoca.data.repository.FlashcardRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.Assert.*;

public class FlashcardViewModelTest {
    // A captured public FOOD response, used only by these unit tests.
    private final List<Flashcard> capturedCards = new Gson().fromJson("["
            + "{\"id\":1,\"english\":\"apple\",\"vietnamese\":\"quả táo\",\"pronunciation\":\"/ˈæpəl/\",\"partOfSpeech\":\"noun\",\"example\":\"I eat an apple every morning.\",\"exampleVi\":\"Tôi ăn một quả táo mỗi sáng.\",\"level\":\"A1\"},"
            + "{\"id\":2,\"english\":\"bread\",\"vietnamese\":\"bánh mì\",\"pronunciation\":\"/bred/\",\"partOfSpeech\":\"noun\",\"example\":\"We buy fresh bread from the bakery.\",\"exampleVi\":\"Chúng tôi mua bánh mì mới ở tiệm bánh.\",\"level\":\"A1\"}]",
            new TypeToken<List<Flashcard>>() {}.getType());

    private PendingRepository repository;
    private FlashcardViewModel model;

    @Before public void setUp() {
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override public void executeOnDiskIO(Runnable runnable) { runnable.run(); }
            @Override public void postToMainThread(Runnable runnable) { runnable.run(); }
            @Override public boolean isMainThread() { return true; }
        });
        repository = new PendingRepository();
        model = new FlashcardViewModel(new SavedStateHandle(), repository);
    }

    @After public void tearDown() {
        model.onCleared();
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test public void invalidTopicDoesNotRequestApi() {
        model.loadFlashcards("../FOOD");
        assertTrue(repository.requests.isEmpty());
        assertNotNull(model.getError().getValue());
        assertFalse(model.getLoading().getValue());
        assertTrue(model.getFlashcards().getValue().isEmpty());
    }

    @Test public void navigationStopsAtBothEnds() {
        model.loadFlashcards("FOOD");
        assertTrue(model.getLoading().getValue());
        assertEquals("FOOD", repository.requests.get(0).topic);
        repository.requests.get(0).callback.onSuccess(capturedCards);
        assertFalse(model.getLoading().getValue());
        model.previous();
        assertEquals(Integer.valueOf(0), model.getCurrentIndex().getValue());
        model.next();
        model.next();
        assertEquals(Integer.valueOf(1), model.getCurrentIndex().getValue());
        model.previous();
        assertEquals(Integer.valueOf(0), model.getCurrentIndex().getValue());
    }

    @Test public void emptyListAndLoadingCannotNavigate() {
        model.loadFlashcards("FOOD");
        model.next();
        assertEquals(Integer.valueOf(0), model.getCurrentIndex().getValue());
        repository.requests.get(0).callback.onSuccess(Collections.emptyList());
        model.next();
        model.previous();
        assertTrue(model.getFlashcards().getValue().isEmpty());
        assertEquals(Integer.valueOf(0), model.getCurrentIndex().getValue());
        assertNull(model.getError().getValue());
    }

    @Test public void changingTopicCancelsAndIgnoresOldResponse() {
        model.loadFlashcards("FOOD");
        PendingRequest old = repository.requests.get(0);
        model.loadFlashcards("ANIMALS");
        assertTrue(old.call.isCanceled());
        old.callback.onSuccess(capturedCards);
        old.callback.onError("obsolete error");
        assertTrue(model.getLoading().getValue());
        assertNull(model.getFlashcards().getValue());
        assertNull(model.getError().getValue());
        repository.requests.get(1).callback.onSuccess(Collections.emptyList());
        assertFalse(model.getLoading().getValue());
        assertTrue(model.getFlashcards().getValue().isEmpty());
    }

    @Test public void retryClearsErrorAndLoadsAgain() {
        model.loadFlashcards("FOOD");
        repository.requests.get(0).callback.onError("Không kết nối được máy chủ.");
        assertNotNull(model.getError().getValue());
        model.loadFlashcards("FOOD");
        assertNull(model.getError().getValue());
        assertTrue(model.getLoading().getValue());
        repository.requests.get(1).callback.onSuccess(capturedCards);
        assertEquals(2, model.getFlashcards().getValue().size());
        assertFalse(model.getLoading().getValue());
    }

    @Test public void restoredPositionIsClampedWhenListShrinks() {
        Map<String, Object> state = new HashMap<>();
        state.put("flashcard_topic", "FOOD");
        state.put("flashcard_index", 29);
        model = new FlashcardViewModel(new SavedStateHandle(state), repository);
        model.loadFlashcards("FOOD");
        repository.requests.get(0).callback.onSuccess(capturedCards);
        assertEquals(Integer.valueOf(1), model.getCurrentIndex().getValue());
        model.loadFlashcards("TRAVEL");
        assertEquals(Integer.valueOf(0), model.getCurrentIndex().getValue());
    }

    @Test public void clearedViewModelCancelsAndIgnoresResponse() {
        model.loadFlashcards("FOOD");
        PendingRequest pending = repository.requests.get(0);
        model.onCleared();
        assertTrue(pending.call.isCanceled());
        pending.callback.onSuccess(capturedCards);
        assertNull(model.getFlashcards().getValue());
    }

    @Test public void cachedCardsKeepNavigationAndExplicitRetryReturnsOnline() {
        model.loadFlashcards("FOOD");
        repository.requests.get(0).callback.onSuccess(capturedCards);
        model.next();
        assertEquals(Integer.valueOf(1), model.getCurrentIndex().getValue());
        model.loadFlashcards("FOOD");
        assertTrue(model.getLoading().getValue());
        repository.requests.get(1).callback.onSuccess(capturedCards);
        assertEquals(Integer.valueOf(1), model.getCurrentIndex().getValue());
    }

    @Test public void rotationKeepsCachedPositionAndDoesNotDuplicateRequest() {
        model.ensureFlashcardsLoaded("FOOD");
        model.ensureFlashcardsLoaded("FOOD");
        assertEquals(1, repository.requests.size());
        repository.requests.get(0).callback.onSuccess(capturedCards);
        model.next();
        model.ensureFlashcardsLoaded("FOOD");
        assertEquals(1, repository.requests.size());
        assertEquals(Integer.valueOf(1), model.getCurrentIndex().getValue());
        model.ensureFlashcardsLoaded("ANIMALS");
        assertEquals(2, repository.requests.size());
        assertEquals(Integer.valueOf(0), model.getCurrentIndex().getValue());
    }

    @Test public void oldCachedResponseCannotReplaceAnotherTopic() {
        model.loadFlashcards("FOOD");
        PendingRequest old = repository.requests.get(0);
        model.loadFlashcards("ANIMALS");
        repository.requests.get(1).callback.onSuccess(Collections.emptyList());
        old.callback.onSuccess(capturedCards);
        assertTrue(model.getFlashcards().getValue().isEmpty());
        assertFalse(model.getLoading().getValue());
        assertNull(model.getError().getValue());
    }

    @Test public void cachedEmptyListIsAValidOfflineResult() {
        model.loadFlashcards("FOOD");
        repository.requests.get(0).callback.onSuccess(Collections.emptyList());
        model.next();
        assertTrue(model.getFlashcards().getValue().isEmpty());
        assertEquals(Integer.valueOf(0), model.getCurrentIndex().getValue());
        assertNull(model.getError().getValue());
    }

    @Test public void savedPositionSurvivesRecreationWithCacheFallback() {
        SavedStateHandle state = new SavedStateHandle();
        model = new FlashcardViewModel(state, repository);
        model.loadFlashcards("FOOD");
        repository.requests.get(0).callback.onSuccess(capturedCards);
        model.next();
        model.onCleared();
        model = new FlashcardViewModel(state, repository);
        model.ensureFlashcardsLoaded("FOOD");
        repository.requests.get(1).callback.onSuccess(capturedCards);
        assertEquals(Integer.valueOf(1), model.getCurrentIndex().getValue());
    }

    @Test public void topicPagingKeepsPreviousNextWithoutLoadingTheWholeTopic() {
        PagedRepository paged = new PagedRepository();
        model = new FlashcardViewModel(new SavedStateHandle(), paged);
        model.loadFlashcards("POS_NOUN");
        assertEquals(1, paged.requests.get(0).page);
        paged.requests.get(0).callback.onSuccess(new FlashcardPage(capturedCards, 1, 100, 102, 2));
        model.next();
        assertEquals(Integer.valueOf(1), model.getCurrentIndex().getValue());
        model.next();
        assertEquals(2, paged.requests.get(1).page);
        List<Flashcard> last = Collections.singletonList(capturedCards.get(0));
        paged.requests.get(1).callback.onSuccess(new FlashcardPage(last, 2, 100, 102, 2));
        assertEquals(101, model.getGlobalPosition());
        assertEquals(102, model.getTopicTotal());
        model.previous();
        assertEquals(1, paged.requests.get(2).page);
        paged.requests.get(2).callback.onSuccess(new FlashcardPage(capturedCards, 1, 100, 102, 2));
        assertEquals(Integer.valueOf(1), model.getCurrentIndex().getValue());
    }

    private static class PendingRepository extends FlashcardRepository {
        final List<PendingRequest> requests = new ArrayList<>();
        PendingRepository() { super(code -> null); }
        @Override public Call<List<Flashcard>> getFlashcards(String topic, FlashcardsCallback callback) {
            PendingRequest pending = new PendingRequest(topic, callback);
            requests.add(pending);
            return pending.call;
        }
    }

    private static class PendingRequest {
        final String topic;
        final FlashcardRepository.FlashcardsCallback callback;
        final PendingCall call = new PendingCall();
        PendingRequest(String topic, FlashcardRepository.FlashcardsCallback callback) {
            this.topic = topic;
            this.callback = callback;
        }
    }

    private static class PagedRepository extends FlashcardRepository {
        final List<PendingPageRequest> requests = new ArrayList<>();
        PagedRepository() { super(code -> null); }
        @Override public Call<?> getFlashcardPage(String topic, int page, int limit, FlashcardPageCallback callback) {
            PendingPageRequest request=new PendingPageRequest(page,callback);requests.add(request);return request.call;
        }
    }
    private static class PendingPageRequest {
        final int page;final FlashcardRepository.FlashcardPageCallback callback;final PendingPageCall call=new PendingPageCall();
        PendingPageRequest(int page,FlashcardRepository.FlashcardPageCallback callback){this.page=page;this.callback=callback;}
    }
    private static class PendingPageCall implements Call<FlashcardPage> {
        private boolean canceled;
        @Override public void cancel(){canceled=true;} @Override public boolean isCanceled(){return canceled;}
        @Override public boolean isExecuted(){return false;} @Override public Response<FlashcardPage> execute(){throw new UnsupportedOperationException();}
        @Override public void enqueue(Callback<FlashcardPage> callback){throw new UnsupportedOperationException();}
        @Override public Call<FlashcardPage> clone(){return new PendingPageCall();}
        @Override public Request request(){return new Request.Builder().url("http://localhost/").build();}
        @Override public Timeout timeout(){return Timeout.NONE;}
    }

    private static class PendingCall implements Call<List<Flashcard>> {
        private boolean canceled;
        @Override public void cancel() { canceled = true; }
        @Override public boolean isCanceled() { return canceled; }
        @Override public boolean isExecuted() { return false; }
        @Override public Response<List<Flashcard>> execute() { throw new UnsupportedOperationException(); }
        @Override public void enqueue(Callback<List<Flashcard>> callback) { throw new UnsupportedOperationException(); }
        @Override public Call<List<Flashcard>> clone() { return new PendingCall(); }
        @Override public Request request() { return new Request.Builder().url("http://localhost/").build(); }
        @Override public Timeout timeout() { return Timeout.NONE; }
    }
}
