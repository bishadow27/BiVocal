package com.example.bivoca.ui.topics;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bivoca.data.model.Flashcard;
import com.example.bivoca.data.model.FlashcardPage;
import com.example.bivoca.data.model.Topic;
import com.example.bivoca.data.repository.FlashcardRepository;
import com.example.bivoca.data.repository.TopicRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import retrofit2.Call;

/** Retains the catalog metadata and bounded preview independently across recreation. */
public class TopicDetailViewModel extends ViewModel {
    private static final int PREVIEW_PAGE = 1;
    private static final int PREVIEW_LIMIT = 20;
    private static final int DISPLAY_LIMIT = 3;
    private static final String METADATA_ERROR =
            "Không thể tải thông tin chủ đề. Vui lòng thử lại.";
    private static final String PREVIEW_ERROR =
            "Không thể tải từ vựng xem trước. Vui lòng thử lại.";

    private final TopicRepository topicRepository;
    private final FlashcardRepository flashcardRepository;
    private final MutableLiveData<Topic> topic = new MutableLiveData<>();
    private final MutableLiveData<Boolean> metadataLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> metadataError = new MutableLiveData<>();
    private final MutableLiveData<List<Flashcard>> preview =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> previewLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> previewError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showUnverifiedWarning = new MutableLiveData<>(false);

    private Call<?> metadataCall;
    private Call<?> previewCall;
    private int metadataVersion;
    private int previewVersion;
    private String topicCode;
    private boolean requested;
    private boolean cleared;

    public TopicDetailViewModel(TopicRepository topicRepository,
                                FlashcardRepository flashcardRepository) {
        this.topicRepository = topicRepository;
        this.flashcardRepository = flashcardRepository;
    }

    public LiveData<Topic> getTopic() { return topic; }
    public LiveData<Boolean> getMetadataLoading() { return metadataLoading; }
    public LiveData<String> getMetadataError() { return metadataError; }
    public LiveData<List<Flashcard>> getPreview() { return preview; }
    public LiveData<Boolean> getPreviewLoading() { return previewLoading; }
    public LiveData<String> getPreviewError() { return previewError; }
    public LiveData<Boolean> getShowUnverifiedWarning() { return showUnverifiedWarning; }

    /** Calling this from a recreated Activity does not restart either request. */
    public void ensureLoaded(String code) {
        if (cleared || (requested && Objects.equals(topicCode, code))) return;
        requested = true;
        topicCode = code;
        invalidateRequests();
        topic.setValue(null);
        metadataError.setValue(null);
        metadataLoading.setValue(false);
        preview.setValue(Collections.emptyList());
        previewError.setValue(null);
        previewLoading.setValue(false);
        showUnverifiedWarning.setValue(false);
        if (!FlashcardRepository.isValidTopicCode(code)) {
            metadataError.setValue("Chủ đề không hợp lệ.");
            return;
        }
        retryMetadata();
        retryPreview();
    }

    public void retryMetadata() {
        if (cleared || !FlashcardRepository.isValidTopicCode(topicCode)) return;
        final int version = ++metadataVersion;
        final String code = topicCode;
        if (metadataCall != null) metadataCall.cancel();
        metadataLoading.setValue(true);
        metadataError.setValue(null);
        metadataCall = topicRepository.getTopics(new TopicRepository.TopicsCallback() {
            @Override public void onSuccess(List<Topic> catalog) {
                if (!isCurrentMetadata(version, code)) return;
                Topic selected = null;
                if (catalog != null) {
                    for (Topic candidate : catalog) {
                        if (candidate != null && code.equals(candidate.getCode())
                                && candidate.getId() > 0 && hasText(candidate.getName())) {
                            selected = candidate;
                            break;
                        }
                    }
                }
                topic.setValue(selected);
                metadataError.setValue(selected == null
                        ? "Không tìm thấy thông tin chủ đề." : null);
                metadataLoading.setValue(false);
            }

            @Override public void onError(String message) {
                if (!isCurrentMetadata(version, code)) return;
                metadataError.setValue(hasText(message) ? message : METADATA_ERROR);
                metadataLoading.setValue(false);
            }
        });
    }

    public void retryPreview() {
        if (cleared || !FlashcardRepository.isValidTopicCode(topicCode)) return;
        final int version = ++previewVersion;
        final String code = topicCode;
        if (previewCall != null) previewCall.cancel();
        preview.setValue(Collections.emptyList());
        showUnverifiedWarning.setValue(false);
        previewLoading.setValue(true);
        previewError.setValue(null);
        previewCall = flashcardRepository.getFlashcardPage(code, PREVIEW_PAGE, PREVIEW_LIMIT,
                new FlashcardRepository.FlashcardPageCallback() {
                    @Override public void onSuccess(FlashcardPage page) {
                        if (!isCurrentPreview(version, code)) return;
                        if (page == null || page.getItems() == null) {
                            onError(PREVIEW_ERROR);
                            return;
                        }
                        List<Flashcard> displayed = new ArrayList<>(DISPLAY_LIMIT);
                        Set<Integer> ids = new HashSet<>();
                        List<Flashcard> items = page.getItems();
                        int count = Math.min(PREVIEW_LIMIT, items.size());
                        for (int i = 0; i < count && displayed.size() < DISPLAY_LIMIT; i++) {
                            Flashcard card = items.get(i);
                            if (card != null && card.getId() > 0 && hasText(card.getEnglish())
                                    && hasText(card.getVietnamese()) && ids.add(card.getId())) {
                                displayed.add(card);
                            }
                        }
                        boolean unverified = false;
                        for (Flashcard card : displayed) {
                            if ("IMPORTED_UNVERIFIED".equals(card.getContentStatus())) {
                                unverified = true;
                                break;
                            }
                        }
                        preview.setValue(Collections.unmodifiableList(displayed));
                        showUnverifiedWarning.setValue(unverified);
                        previewError.setValue(null);
                        previewLoading.setValue(false);
                    }

                    @Override public void onError(String message) {
                        if (!isCurrentPreview(version, code)) return;
                        preview.setValue(Collections.emptyList());
                        showUnverifiedWarning.setValue(false);
                        previewError.setValue(hasText(message) ? message : PREVIEW_ERROR);
                        previewLoading.setValue(false);
                    }
                });
    }

    private boolean isCurrentMetadata(int version, String code) {
        return !cleared && version == metadataVersion && Objects.equals(code, topicCode);
    }

    private boolean isCurrentPreview(int version, String code) {
        return !cleared && version == previewVersion && Objects.equals(code, topicCode);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /** Invalidate before cancel: even a cancellation callback cannot update the new state. */
    private void invalidateRequests() {
        metadataVersion++;
        previewVersion++;
        if (metadataCall != null) metadataCall.cancel();
        if (previewCall != null) previewCall.cancel();
        metadataCall = null;
        previewCall = null;
    }

    @Override protected void onCleared() {
        cleared = true;
        invalidateRequests();
    }
}
