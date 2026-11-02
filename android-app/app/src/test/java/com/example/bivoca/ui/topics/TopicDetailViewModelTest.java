package com.example.bivoca.ui.topics;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;

import com.example.bivoca.data.api.FlashcardApi;
import com.example.bivoca.data.model.Flashcard;
import com.example.bivoca.data.model.FlashcardPage;
import com.example.bivoca.data.model.Topic;
import com.example.bivoca.data.repository.FlashcardRepository;
import com.example.bivoca.data.repository.TopicRepository;
import com.google.gson.Gson;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.Assert.*;

/** API responses are controlled here only; production repositories remain unchanged. */
public class TopicDetailViewModelTest {
    private PendingTopics topics;
    private PendingFlashcards flashcards;
    private TopicDetailViewModel model;

    @Before public void setUp() {
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override public void executeOnDiskIO(Runnable runnable) { runnable.run(); }
            @Override public void postToMainThread(Runnable runnable) { runnable.run(); }
            @Override public boolean isMainThread() { return true; }
        });
        topics = new PendingTopics();
        flashcards = new PendingFlashcards();
        model = new TopicDetailViewModel(topics, flashcards);
    }

    @After public void tearDown() {
        model.onCleared();
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test public void initialStateIsSafeAndLoadingUsesOnlyBoundedFirstPage() {
        assertNull(model.getTopic().getValue());
        assertFalse(model.getMetadataLoading().getValue());
        assertFalse(model.getPreviewLoading().getValue());
        assertTrue(model.getPreview().getValue().isEmpty());
        assertFalse(model.getShowUnverifiedWarning().getValue());
        model.ensureLoaded("FOOD");
        assertEquals(1, topics.requests.size());
        assertEquals(1, flashcards.requests.size());
        PreviewRequest request = flashcards.requests.get(0);
        assertEquals("FOOD", request.code);
        assertEquals(1, request.page);
        assertEquals(20, request.limit);
        assertTrue(model.getMetadataLoading().getValue());
        assertTrue(model.getPreviewLoading().getValue());
    }

    @Test public void metadataSelectsRequestedCatalogEntryAndKeepsApiValues() {
        model.ensureLoaded("FOOD");
        Topic selected = new Topic(2, "FOOD", "Food", "API description", null, 527);
        topics.requests.get(0).callback.onSuccess(Arrays.asList(topic("WORK"), selected));
        assertSame(selected, model.getTopic().getValue());
        assertEquals(527, model.getTopic().getValue().getFlashcardCount());
        assertFalse(model.getMetadataLoading().getValue());
        assertNull(model.getMetadataError().getValue());
        assertTrue(model.getPreviewLoading().getValue());
    }

    @Test public void previewSkipsNullInvalidAndDuplicateCardsThenCapsAtThree() {
        model.ensureLoaded("FOOD");
        List<Flashcard> input = Arrays.asList(null, card(0, "zero", "nghia", null),
                card(1, " ", "nghia", null), card(2, "valid", null, null),
                card(3, "first", "mot", null), card(3, "duplicate", "hai", null),
                card(4, "second", "hai", null), card(5, "third", "ba", null),
                card(6, "fourth", "bon", "IMPORTED_UNVERIFIED"));
        flashcards.requests.get(0).callback.onSuccess(page(input));
        assertEquals(3, model.getPreview().getValue().size());
        assertEquals(3, model.getPreview().getValue().get(0).getId());
        assertEquals(4, model.getPreview().getValue().get(1).getId());
        assertEquals(5, model.getPreview().getValue().get(2).getId());
        assertFalse(model.getShowUnverifiedWarning().getValue());
        assertFalse(model.getPreviewLoading().getValue());
        assertNull(model.getPreviewError().getValue());
    }

    @Test public void warningUsesExactStatusOfDisplayedCardsOnly() {
        model.ensureLoaded("FOOD");
        flashcards.requests.get(0).callback.onSuccess(page(Arrays.asList(
                card(1, "first", "mot", "IMPORTED_UNVERIFIED"),
                card(2, "second", "hai", "VERIFIED"))));
        assertTrue(model.getShowUnverifiedWarning().getValue());
        model.retryPreview();
        flashcards.requests.get(1).callback.onSuccess(page(Arrays.asList(
                card(1, "first", "mot", "imported_unverified"),
                card(2, "second", "hai", "IMPORTED_UNVERIFIED "),
                card(3, "third", "ba", null),
                card(4, "hidden", "bon", "IMPORTED_UNVERIFIED"))));
        assertFalse(model.getShowUnverifiedWarning().getValue());
    }

    @Test public void invalidOrDuplicateUnverifiedCardsCannotCreateWarning() {
        model.ensureLoaded("FOOD");
        flashcards.requests.get(0).callback.onSuccess(page(Arrays.asList(
                card(1, "first", "mot", "VERIFIED"),
                card(1, "duplicate", "mot", "IMPORTED_UNVERIFIED"),
                card(2, null, "hai", "IMPORTED_UNVERIFIED"))));
        assertEquals(1, model.getPreview().getValue().size());
        assertFalse(model.getShowUnverifiedWarning().getValue());
    }

    @Test public void oversizedResponseNeverSelectsCardsBeyondRequestedTwentyItems() {
        model.ensureLoaded("FOOD");
        List<Flashcard> oversized = new ArrayList<>(Collections.nCopies(20, null));
        oversized.add(card(21, "outside page", "ngoai trang", "IMPORTED_UNVERIFIED"));
        flashcards.requests.get(0).callback.onSuccess(page(oversized));
        assertTrue(model.getPreview().getValue().isEmpty());
        assertFalse(model.getShowUnverifiedWarning().getValue());
    }

    @Test public void emptyPreviewIsSuccessfulAndNeverWarns() {
        model.ensureLoaded("FOOD");
        flashcards.requests.get(0).callback.onSuccess(page(Collections.emptyList()));
        assertTrue(model.getPreview().getValue().isEmpty());
        assertFalse(model.getPreviewLoading().getValue());
        assertNull(model.getPreviewError().getValue());
        assertFalse(model.getShowUnverifiedWarning().getValue());
    }

    @Test public void malformedPageBecomesSafeErrorWithoutWarning() {
        model.ensureLoaded("FOOD");
        flashcards.requests.get(0).callback.onSuccess(null);
        assertTrue(model.getPreview().getValue().isEmpty());
        assertNotNull(model.getPreviewError().getValue());
        assertFalse(model.getPreviewLoading().getValue());
        assertFalse(model.getShowUnverifiedWarning().getValue());
        model.retryPreview();
        flashcards.requests.get(1).callback.onSuccess(new FlashcardPage(null, 1, 20, 0, 0));
        assertNotNull(model.getPreviewError().getValue());
        assertFalse(model.getShowUnverifiedWarning().getValue());
    }

    @Test public void previewFailureAndRetryPreserveMetadataAndUseBoundedParams() {
        model.ensureLoaded("FOOD");
        Topic selected = topic("FOOD");
        topics.requests.get(0).callback.onSuccess(Collections.singletonList(selected));
        flashcards.requests.get(0).callback.onError("preview failure");
        assertSame(selected, model.getTopic().getValue());
        assertNull(model.getMetadataError().getValue());
        assertEquals("preview failure", model.getPreviewError().getValue());
        assertFalse(model.getShowUnverifiedWarning().getValue());
        model.retryPreview();
        assertEquals(1, topics.requests.size());
        assertEquals(2, flashcards.requests.size());
        assertEquals(1, flashcards.requests.get(1).page);
        assertEquals(20, flashcards.requests.get(1).limit);
        assertNull(model.getPreviewError().getValue());
        assertTrue(model.getPreviewLoading().getValue());
        flashcards.requests.get(1).callback.onSuccess(page(Collections.singletonList(
                card(1, "first", "mot", null))));
        assertSame(selected, model.getTopic().getValue());
        assertEquals(1, model.getPreview().getValue().size());
    }

    @Test public void metadataFailureAndRetryPreservePreviewAndDoNotReloadIt() {
        model.ensureLoaded("FOOD");
        flashcards.requests.get(0).callback.onSuccess(page(Collections.singletonList(
                card(1, "first", "mot", "IMPORTED_UNVERIFIED"))));
        topics.requests.get(0).callback.onError("metadata failure");
        assertEquals("metadata failure", model.getMetadataError().getValue());
        assertFalse(model.getMetadataLoading().getValue());
        assertEquals(1, model.getPreview().getValue().size());
        model.retryMetadata();
        assertEquals(2, topics.requests.size());
        assertEquals(1, flashcards.requests.size());
        assertNull(model.getMetadataError().getValue());
        assertTrue(model.getMetadataLoading().getValue());
        topics.requests.get(1).callback.onSuccess(Collections.singletonList(topic("FOOD")));
        assertEquals("FOOD", model.getTopic().getValue().getCode());
        assertTrue(model.getShowUnverifiedWarning().getValue());
    }

    @Test public void repeatedEnsureReusesInflightAndCompletedStateAcrossRecreation() {
        model.ensureLoaded("FOOD");
        model.ensureLoaded("FOOD");
        assertEquals(1, topics.requests.size());
        assertEquals(1, flashcards.requests.size());
        topics.requests.get(0).callback.onSuccess(Collections.singletonList(topic("FOOD")));
        flashcards.requests.get(0).callback.onSuccess(page(Collections.emptyList()));
        model.ensureLoaded("FOOD");
        assertEquals(1, topics.requests.size());
        assertEquals(1, flashcards.requests.size());
        assertEquals("FOOD", model.getTopic().getValue().getCode());
    }

    @Test public void recreationAfterFailuresWaitsForExplicitRetry() {
        model.ensureLoaded("FOOD");
        topics.requests.get(0).callback.onError("metadata failure");
        flashcards.requests.get(0).callback.onError("preview failure");
        model.ensureLoaded("FOOD");
        assertEquals(1, topics.requests.size());
        assertEquals(1, flashcards.requests.size());
        assertEquals("metadata failure", model.getMetadataError().getValue());
        assertEquals("preview failure", model.getPreviewError().getValue());
    }

    @Test public void metadataRetryRejectsObsoleteSuccessAndFailure() {
        model.ensureLoaded("FOOD");
        TopicRequest obsolete = topics.requests.get(0);
        model.retryMetadata();
        assertTrue(obsolete.call.isCanceled());
        Topic latest = new Topic(2, "FOOD", "Latest", "Latest description", null, 3);
        topics.requests.get(1).callback.onSuccess(Collections.singletonList(latest));
        obsolete.callback.onSuccess(Collections.singletonList(topic("FOOD")));
        obsolete.callback.onError("old metadata failure");
        assertSame(latest, model.getTopic().getValue());
        assertNull(model.getMetadataError().getValue());
        assertFalse(model.getMetadataLoading().getValue());
        assertFalse(flashcards.requests.get(0).call.isCanceled());
    }

    @Test public void previewRetryRejectsObsoleteSuccessAndFailure() {
        model.ensureLoaded("FOOD");
        PreviewRequest obsolete = flashcards.requests.get(0);
        model.retryPreview();
        assertTrue(obsolete.call.isCanceled());
        flashcards.requests.get(1).callback.onSuccess(page(Collections.singletonList(
                card(2, "latest", "hai", null))));
        obsolete.callback.onSuccess(page(Collections.singletonList(
                card(1, "old", "mot", "IMPORTED_UNVERIFIED"))));
        obsolete.callback.onError("old preview failure");
        assertEquals(2, model.getPreview().getValue().get(0).getId());
        assertNull(model.getPreviewError().getValue());
        assertFalse(model.getPreviewLoading().getValue());
        assertFalse(model.getShowUnverifiedWarning().getValue());
        assertFalse(topics.requests.get(0).call.isCanceled());
    }

    @Test public void switchingCodeCancelsBothAndClearsPriorTopicPreviewAndWarning() {
        model.ensureLoaded("FOOD");
        TopicRequest oldMetadata = topics.requests.get(0);
        PreviewRequest oldPreview = flashcards.requests.get(0);
        oldMetadata.callback.onSuccess(Collections.singletonList(topic("FOOD")));
        oldPreview.callback.onSuccess(page(Collections.singletonList(
                card(1, "first", "mot", "IMPORTED_UNVERIFIED"))));
        model.ensureLoaded("WORK");
        assertTrue(oldMetadata.call.isCanceled());
        assertTrue(oldPreview.call.isCanceled());
        assertNull(model.getTopic().getValue());
        assertTrue(model.getPreview().getValue().isEmpty());
        assertFalse(model.getShowUnverifiedWarning().getValue());
        assertEquals("WORK", flashcards.requests.get(1).code);
        topics.requests.get(1).callback.onSuccess(Collections.singletonList(topic("WORK")));
        flashcards.requests.get(1).callback.onSuccess(page(Collections.emptyList()));
        oldMetadata.callback.onSuccess(Collections.singletonList(topic("FOOD")));
        oldMetadata.callback.onError("old metadata failure");
        oldPreview.callback.onSuccess(page(Collections.singletonList(
                card(1, "old", "mot", "IMPORTED_UNVERIFIED"))));
        oldPreview.callback.onError("old preview failure");
        assertEquals("WORK", model.getTopic().getValue().getCode());
        assertTrue(model.getPreview().getValue().isEmpty());
        assertNull(model.getMetadataError().getValue());
        assertNull(model.getPreviewError().getValue());
        assertFalse(model.getShowUnverifiedWarning().getValue());
    }

    @Test public void clearedModelCancelsAndInvalidatesBothChannels() {
        model.ensureLoaded("FOOD");
        TopicRequest metadata = topics.requests.get(0);
        PreviewRequest preview = flashcards.requests.get(0);
        model.onCleared();
        assertTrue(metadata.call.isCanceled());
        assertTrue(preview.call.isCanceled());
        metadata.callback.onSuccess(Collections.singletonList(topic("FOOD")));
        metadata.callback.onError("late metadata failure");
        preview.callback.onSuccess(page(Collections.singletonList(
                card(1, "late", "mot", "IMPORTED_UNVERIFIED"))));
        preview.callback.onError("late preview failure");
        assertNull(model.getTopic().getValue());
        assertTrue(model.getPreview().getValue().isEmpty());
        assertNull(model.getMetadataError().getValue());
        assertNull(model.getPreviewError().getValue());
        assertFalse(model.getShowUnverifiedWarning().getValue());
    }

    @Test public void invalidCodesProduceSafeMetadataErrorWithoutRepositoryRequests() {
        for (String invalid : new String[]{null, "", " ", "food", "FOOD/OTHER"}) {
            model.ensureLoaded(invalid);
            assertNull(model.getTopic().getValue());
            assertNotNull(model.getMetadataError().getValue());
            assertFalse(model.getMetadataLoading().getValue());
            assertFalse(model.getPreviewLoading().getValue());
            assertTrue(model.getPreview().getValue().isEmpty());
            assertFalse(model.getShowUnverifiedWarning().getValue());
        }
        model.retryMetadata();
        model.retryPreview();
        assertTrue(topics.requests.isEmpty());
        assertTrue(flashcards.requests.isEmpty());
    }

    @Test public void missingOrNullCatalogReportsErrorWhilePreviewRemainsIndependent() {
        model.ensureLoaded("FOOD");
        topics.requests.get(0).callback.onSuccess(Collections.singletonList(topic("WORK")));
        assertNull(model.getTopic().getValue());
        assertNotNull(model.getMetadataError().getValue());
        assertFalse(model.getMetadataLoading().getValue());
        assertTrue(model.getPreviewLoading().getValue());
        model.retryMetadata();
        topics.requests.get(1).callback.onSuccess(null);
        assertNotNull(model.getMetadataError().getValue());
        assertFalse(model.getMetadataLoading().getValue());
        flashcards.requests.get(0).callback.onSuccess(page(Collections.emptyList()));
        assertNull(model.getPreviewError().getValue());
        assertFalse(model.getShowUnverifiedWarning().getValue());
    }

    private static Topic topic(String code) {
        return new Topic(1, code, code + " name", "Catalog description", null, 20);
    }

    private static Flashcard card(int id, String english, String vietnamese, String status) {
        // Gson is already a production dependency and sets the DTO's read-only status field.
        Gson gson = new Gson();
        Flashcard base = new Flashcard(id, english, vietnamese, null, null, null, null, null);
        com.google.gson.JsonObject json = gson.toJsonTree(base).getAsJsonObject();
        json.addProperty("contentStatus", status);
        return gson.fromJson(json, Flashcard.class);
    }

    private static FlashcardPage page(List<Flashcard> items) {
        return new FlashcardPage(items, 1, 20, items.size(), 1);
    }

    private static final class PendingTopics extends TopicRepository {
        final List<TopicRequest> requests = new ArrayList<>();
        PendingTopics() { super(() -> null); }
        @Override public Call<List<Topic>> getTopics(TopicsCallback callback) {
            TopicRequest request = new TopicRequest(callback);
            requests.add(request);
            return request.call;
        }
    }

    private static final class PendingFlashcards extends FlashcardRepository {
        final List<PreviewRequest> requests = new ArrayList<>();
        PendingFlashcards() { super((FlashcardApi) null); }
        @Override public Call<?> getFlashcardPage(String code, int page, int limit,
                                                FlashcardPageCallback callback) {
            PreviewRequest request = new PreviewRequest(code, page, limit, callback);
            requests.add(request);
            return request.call;
        }
        @Override public Call<List<Flashcard>> getFlashcards(String code, FlashcardsCallback callback) {
            throw new AssertionError("Detail must use the bounded page API");
        }
    }

    private static final class TopicRequest {
        final TopicRepository.TopicsCallback callback;
        final PendingCall<List<Topic>> call = new PendingCall<>();
        TopicRequest(TopicRepository.TopicsCallback callback) { this.callback = callback; }
    }

    private static final class PreviewRequest {
        final String code;
        final int page;
        final int limit;
        final FlashcardRepository.FlashcardPageCallback callback;
        final PendingCall<FlashcardPage> call = new PendingCall<>();
        PreviewRequest(String code, int page, int limit,
                       FlashcardRepository.FlashcardPageCallback callback) {
            this.code = code;
            this.page = page;
            this.limit = limit;
            this.callback = callback;
        }
    }

    private static final class PendingCall<T> implements Call<T> {
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
