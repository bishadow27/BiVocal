package com.example.bivoca.ui.flashcard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.bivoca.data.model.Flashcard;
import com.example.bivoca.data.model.FlashcardPage;
import com.example.bivoca.data.model.ProgressDto;
import com.example.bivoca.data.repository.FlashcardRepository;
import com.example.bivoca.data.repository.ProgressRepository;
import com.example.bivoca.data.repository.LearningRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;

public class FlashcardViewModel extends ViewModel {
    private static final String STATE_TOPIC = "flashcard_topic";
    private static final String STATE_INDEX = "flashcard_index";

    private final FlashcardRepository repository;
    private final ProgressRepository progressRepository;
    private final LearningRepository learningRepository;
    private final SavedStateHandle savedState;
    private final MutableLiveData<List<Flashcard>> flashcards = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> reviewPending = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> reviewCompleted = new MutableLiveData<>(false);
    private final MutableLiveData<String> reviewError = new MutableLiveData<>();
    private final MutableLiveData<Integer> sessionTotal = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> sessionProcessed = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> currentIndex;
    private Call<?> activeCall;
    private Call<ProgressDto> reviewCall;
    private int requestVersion;
    private int reviewVersion;
    private boolean requested;
    private String requestedTopic;
    private static final int TOPIC_PAGE_SIZE = 100;
    private int topicPage = 1;
    private int topicTotalPages = 1;
    private int topicTotal;

    FlashcardViewModel(SavedStateHandle savedState, FlashcardRepository repository) {
        this(savedState, repository, null, null);
    }

    FlashcardViewModel(SavedStateHandle savedState, FlashcardRepository repository,
                       ProgressRepository progressRepository) {
        this(savedState, repository, progressRepository, null);
    }

    FlashcardViewModel(SavedStateHandle savedState, FlashcardRepository repository,
                       ProgressRepository progressRepository, LearningRepository learningRepository) {
        this.savedState = savedState;
        this.repository = repository;
        this.progressRepository = progressRepository;
        this.learningRepository = learningRepository;
        currentIndex = savedState.getLiveData(STATE_INDEX, 0);
    }

    public LiveData<List<Flashcard>> getFlashcards() { return flashcards; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<Integer> getCurrentIndex() { return currentIndex; }
    public LiveData<Boolean> getReviewPending() { return reviewPending; }
    public LiveData<Boolean> getReviewCompleted() { return reviewCompleted; }
    public LiveData<String> getReviewError() { return reviewError; }
    public LiveData<Integer> getSessionTotal() { return sessionTotal; }
    public LiveData<Integer> getSessionProcessed() { return sessionProcessed; }
    private void resetSession() { sessionTotal.setValue(0); sessionProcessed.setValue(0); }

    public void ensureDueLoaded() {
        if (!requested || !"__due__".equals(requestedTopic)) loadDueCards();
    }

    public void ensureNewLoaded(int limit) { if (!requested || !"__new__".equals(requestedTopic)) loadNewCards(limit); }
    public void loadNewCards(int limit) {
        if (learningRepository == null || Boolean.TRUE.equals(reviewPending.getValue())) return;
        resetSession();
        requested=true; requestedTopic="__new__"; int version=++requestVersion;
        if(activeCall!=null)activeCall.cancel(); loading.setValue(true); error.setValue(null);
        reviewError.setValue(null); reviewCompleted.setValue(false); flashcards.setValue(null);
        currentIndex.setValue(0); savedState.set(STATE_TOPIC,"__new__");
        activeCall=learningRepository.getNewCards(Math.max(1,Math.min(20,limit)),new LearningRepository.Result<List<Flashcard>>(){
            @Override public void onSuccess(List<Flashcard> value){if(version!=requestVersion)return;sessionTotal.setValue(value.size());flashcards.setValue(value);loading.setValue(false);}
            @Override public void onError(String message){if(version!=requestVersion)return;error.setValue(message);loading.setValue(false);}
        });
    }

    public void loadDueCards() {
        if (progressRepository == null || Boolean.TRUE.equals(reviewPending.getValue())) return;
        resetSession();
        requested = true;
        requestedTopic = "__due__";
        int version = ++requestVersion;
        if (activeCall != null) activeCall.cancel();
        activeCall = null;
        loading.setValue(true);
        error.setValue(null);
        reviewError.setValue(null);
        reviewCompleted.setValue(false);
        flashcards.setValue(null);
        currentIndex.setValue(0);
        savedState.set(STATE_TOPIC, "__due__");
        activeCall = progressRepository.getDueCards(new ProgressRepository.DueCardsCallback() {
            @Override public void onSuccess(List<Flashcard> cards) {
                if (version != requestVersion) return;
                sessionTotal.setValue(cards.size());
                flashcards.setValue(cards);
                loading.setValue(false);
            }
            @Override public void onError(String message) {
                if (version != requestVersion) return;
                error.setValue(message);
                loading.setValue(false);
            }
        });
    }

    public void submitDueReview(boolean correct) {
        if (progressRepository == null || Boolean.TRUE.equals(loading.getValue())
                || Boolean.TRUE.equals(reviewPending.getValue())) return;
        List<Flashcard> cards = flashcards.getValue();
        if (cards == null || cards.isEmpty()) return;
        Flashcard current = cards.get(0);
        int version = ++reviewVersion;
        int listVersion = requestVersion;
        reviewPending.setValue(true);
        reviewError.setValue(null);
        reviewCall = progressRepository.submitReview(current.getId(), correct,
                new ProgressRepository.ProgressCallback() {
            @Override public void onSuccess(ProgressDto progress) {
                if (version != reviewVersion || listVersion != requestVersion) return;
                List<Flashcard> latest = flashcards.getValue();
                if (latest == null || latest.isEmpty() || latest.get(0).getId() != current.getId()
                        || progress.getFlashcardId() != current.getId()) return;
                List<Flashcard> remaining = new ArrayList<>(latest);
                remaining.remove(0);
                sessionProcessed.setValue(sessionProcessed.getValue() + 1);
                reviewPending.setValue(false);
                reviewCompleted.setValue(remaining.isEmpty());
                flashcards.setValue(remaining);
                currentIndex.setValue(0);
            }
            @Override public void onError(String message) {
                if (version != reviewVersion || listVersion != requestVersion) return;
                reviewPending.setValue(false);
                reviewError.setValue(message);
            }
        });
    }

    public void ensureFlashcardsLoaded(String topicCode) {
        if (!requested || !java.util.Objects.equals(topicCode, requestedTopic)) {
            loadFlashcards(topicCode);
        }
    }

    public void loadFlashcards(String topicCode) {
        Integer savedIndex=currentIndex.getValue();
        int desired=java.util.Objects.equals(topicCode,savedState.get(STATE_TOPIC))&&savedIndex!=null?savedIndex:0;
        loadTopicPage(topicCode, 1, desired);
    }

    private void loadTopicPage(String topicCode, int page, int desiredIndex) {
        requested = true;
        requestedTopic = topicCode;
        int version = ++requestVersion;
        if (activeCall != null) activeCall.cancel();
        activeCall = null;
        loading.setValue(true);
        if (!FlashcardRepository.isValidTopicCode(topicCode)) {
            error.setValue("Mã chủ đề không hợp lệ. Hãy quay lại chọn chủ đề.");
            flashcards.setValue(Collections.emptyList());
            currentIndex.setValue(0);
            loading.setValue(false);
            return;
        }

        error.setValue(null);
        flashcards.setValue(null);
        if (!topicCode.equals(savedState.get(STATE_TOPIC))) currentIndex.setValue(0);
        savedState.set(STATE_TOPIC, topicCode);
        activeCall = repository.getFlashcardPage(topicCode, page, TOPIC_PAGE_SIZE, new FlashcardRepository.FlashcardPageCallback() {
            @Override
            public void onSuccess(FlashcardPage result) {
                if (version != requestVersion) return;
                List<Flashcard> items = result.getItems();
                topicPage = result.getPage(); topicTotalPages = Math.max(1, result.getTotalPages()); topicTotal = result.getTotal();
                int index = desiredIndex < 0 ? items.size() - 1 : desiredIndex;
                currentIndex.setValue(Math.max(0, Math.min(index, items.size() - 1)));
                flashcards.setValue(items);
                loading.setValue(false);
            }

            @Override
            public void onError(String message) {
                if (version != requestVersion) return;
                error.setValue(message);
                loading.setValue(false);
            }
        });
    }

    public void previous() { move(-1); }
    public void next() { move(1); }

    public boolean canPrevious() {
        Integer index=currentIndex.getValue();return topicPage>1||(index!=null&&index>0);
    }
    public boolean canNext() {
        List<Flashcard> cards=flashcards.getValue();Integer index=currentIndex.getValue();
        return cards!=null&&index!=null&&(index<cards.size()-1||topicPage<topicTotalPages);
    }
    public int getGlobalPosition() { Integer index=currentIndex.getValue();return (topicPage-1)*TOPIC_PAGE_SIZE+(index==null?0:index)+1; }
    public int getTopicTotal() { return topicTotal; }

    private void move(int direction) {
        List<Flashcard> cards = flashcards.getValue();
        if (Boolean.TRUE.equals(loading.getValue()) || cards == null || cards.isEmpty()) return;
        int index = currentIndex.getValue() == null ? 0 : currentIndex.getValue();
        int next = index + direction;
        if (next >= 0 && next < cards.size()) currentIndex.setValue(next);
        else if (direction > 0 && topicPage < topicTotalPages) loadTopicPage(requestedTopic, topicPage + 1, 0);
        else if (direction < 0 && topicPage > 1) loadTopicPage(requestedTopic, topicPage - 1, -1);
    }

    @Override
    protected void onCleared() {
        requestVersion++;
        reviewVersion++;
        if (activeCall != null) activeCall.cancel();
        if (reviewCall != null) reviewCall.cancel();
    }
}
