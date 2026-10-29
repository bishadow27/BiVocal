package com.example.bivoca.ui.quiz;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import com.example.bivoca.data.model.QuizAnswerResponse;
import com.example.bivoca.data.model.QuizQuestion;
import com.example.bivoca.data.repository.QuizRepository;

import java.util.List;

import retrofit2.Call;

/**
 * ViewModel cho QuizActivity.
 * Vòng đời: LOADING_QUESTIONS → QUESTION → SUBMITTING → RESULT → (câu tiếp / FINISHED)
 * Không tự chấm điểm — backend trả kết quả đúng/sai.
 *
 * topicCode được lưu qua SavedStateHandle để tồn tại sau process death.
 */
public class QuizViewModel extends AndroidViewModel {

    /** Trạng thái toàn bộ màn hình quiz */
    public enum QuizState {
        LOADING_QUESTIONS,  // Đang tải câu hỏi
        QUESTION,           // Đang hiển thị câu hỏi, chờ chọn đáp án
        SUBMITTING,         // Đang gửi đáp án lên backend
        RESULT,             // Hiển thị kết quả một câu (đúng/sai)
        FINISHED,           // Hết câu hỏi — hiển thị màn hình tổng kết
        ERROR               // Lỗi tải câu hỏi không thể tiếp tục
    }

    static final String KEY_TOPIC_CODE = "quiz_topic_code";

    private final QuizRepository repository;
    private final SavedStateHandle savedState;

    private final MutableLiveData<QuizState> state = new MutableLiveData<>(QuizState.LOADING_QUESTIONS);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentIndex = new MutableLiveData<>(0);
    private final MutableLiveData<QuizAnswerResponse> lastAnswer = new MutableLiveData<>();

    private List<QuizQuestion> questions;

    private int correctCount = 0;
    private int answeredCount = 0;

    @Nullable private Call<?> activeCall;
    private int requestVersion = 0;

    /** Constructor gọi bởi SavedStateViewModelFactory (Android tự inject SavedStateHandle). */
    public QuizViewModel(@NonNull Application application, @NonNull SavedStateHandle savedState) {
        super(application);
        this.savedState = savedState;
        this.repository = new QuizRepository(application.getApplicationContext());
    }

    // Getters

    public LiveData<QuizState> getState() { return state; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Integer> getCurrentIndex() { return currentIndex; }
    public LiveData<QuizAnswerResponse> getLastAnswer() { return lastAnswer; }

    @Nullable
    public QuizQuestion getCurrentQuestion() {
        Integer idx = currentIndex.getValue();
        if (questions == null || idx == null || idx < 0 || idx >= questions.size()) return null;
        return questions.get(idx);
    }

    public int getQuestionCount() { return questions == null ? 0 : questions.size(); }
    public int getCorrectCount() { return correctCount; }
    public int getAnsweredCount() { return answeredCount; }

    // Actions

    /**
     * Gọi từ Activity khi khởi động — chỉ tải nếu chưa có câu hỏi.
     * topicCode: null = tất cả chủ đề.
     */
    public void initIfNeeded(@Nullable String topicCode) {
        // Lưu topic nếu mới
        if (topicCode != null) savedState.set(KEY_TOPIC_CODE, topicCode);
        // Chỉ load nếu chưa có dữ liệu (tránh load lại sau rotation)
        QuizState current = state.getValue();
        if (current == QuizState.LOADING_QUESTIONS && (questions == null || questions.isEmpty())) {
            loadQuestions();
        }
    }

    /** Người dùng nhấn Thử lại sau khi gặp lỗi tải câu hỏi. */
    public void retry() {
        loadQuestions();
    }

    private void loadQuestions() {
        cancelActiveCall();
        int version = ++requestVersion;
        state.setValue(QuizState.LOADING_QUESTIONS);
        errorMessage.setValue(null);
        String topicCode = savedState.get(KEY_TOPIC_CODE);

        activeCall = repository.loadQuestions(topicCode, 10,
                new QuizRepository.QuestionsCallback() {
                    @Override
                    public void onSuccess(List<QuizQuestion> result) {
                        if (version != requestVersion) return;
                        questions = result;
                        correctCount = 0;
                        answeredCount = 0;
                        currentIndex.setValue(0);
                        state.setValue(QuizState.QUESTION);
                    }

                    @Override
                    public void onError(String message) {
                        if (version != requestVersion) return;
                        errorMessage.setValue(message);
                        state.setValue(QuizState.ERROR);
                    }
                });
    }

    /**
     * Người dùng chọn một đáp án.
     * @param selectedFlashcardId ID flashcard ứng với lựa chọn
     */
    public void submitAnswer(int selectedFlashcardId) {
        QuizQuestion current = getCurrentQuestion();
        if (current == null) return;
        if (state.getValue() != QuizState.QUESTION) return;

        cancelActiveCall();
        int version = ++requestVersion;
        errorMessage.setValue(null);
        state.setValue(QuizState.SUBMITTING);

        activeCall = repository.submitAnswer(
                current.getFlashcardId(),
                selectedFlashcardId,
                new QuizRepository.AnswerCallback() {
                    @Override
                    public void onSuccess(QuizAnswerResponse response) {
                        if (version != requestVersion) return;
                        answeredCount++;
                        if (response.isCorrect()) correctCount++;
                        lastAnswer.setValue(response);
                        state.setValue(QuizState.RESULT);
                    }

                    @Override
                    public void onError(String message) {
                        if (version != requestVersion) return;
                        // Thất bại → quay về QUESTION để người dùng thử lại
                        errorMessage.setValue(message);
                        state.setValue(QuizState.QUESTION);
                    }
                });
    }

    /** Người dùng nhấn Tiếp tục sau khi xem kết quả một câu. */
    public void nextQuestion() {
        if (state.getValue() != QuizState.RESULT) return;
        Integer idx = currentIndex.getValue();
        if (idx == null) return;
        int next = idx + 1;
        if (questions == null || next >= questions.size()) {
            state.setValue(QuizState.FINISHED);
        } else {
            currentIndex.setValue(next);
            lastAnswer.setValue(null);
            state.setValue(QuizState.QUESTION);
        }
    }

    /** Người dùng muốn chơi lại. */
    public void playAgain() {
        loadQuestions();
    }

    private void cancelActiveCall() {
        if (activeCall != null) {
            activeCall.cancel();
            activeCall = null;
        }
    }

    @Override
    protected void onCleared() {
        requestVersion++;
        cancelActiveCall();
    }
}
