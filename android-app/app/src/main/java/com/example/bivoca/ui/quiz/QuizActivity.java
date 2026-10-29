package com.example.bivoca.ui.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.bivoca.R;
import com.example.bivoca.data.model.QuizAnswerResponse;
import com.example.bivoca.data.model.QuizOption;
import com.example.bivoca.data.model.QuizQuestion;
import com.example.bivoca.ui.home.HomeActivity;
import com.example.bivoca.ui.navigation.NavigationDrawerController;
import com.example.bivoca.ui.navigation.NavigationRoute;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/**
 * Màn hình quiz.
 * Nhận topic code từ Intent (tùy chọn).
 * Điều hướng: FlashcardActivity hoặc HomeActivity → QuizActivity.
 */
public class QuizActivity extends AppCompatActivity {

    public static final String EXTRA_TOPIC_CODE = "com.example.bivoca.QUIZ_TOPIC_CODE";
    public static final String EXTRA_TOPIC_NAME = "com.example.bivoca.QUIZ_TOPIC_NAME";

    private QuizViewModel viewModel;

    // Views
    private TextView textQuizProgress;
    private LinearLayout scoreBar;
    private TextView textScoreCorrect, textScoreTotal;
    private ProgressBar progressQuiz, progressSubmit;
    private TextView textQuizError;
    private MaterialButton buttonQuizRetry;
    private MaterialCardView cardQuestion;
    private TextView textQuizPrompt;
    private LinearLayout optionsContainer;
    private MaterialButton[] optionButtons;
    private MaterialCardView cardResult;
    private TextView textResultVerdict, textResultCorrectAnswer;
    private LinearLayout finishedContainer;
    private TextView textFinalTotal, textFinalCorrect, textFinalWrong, textFinalAccuracy;
    private MaterialButton buttonPlayAgain, buttonQuizDone, buttonContinue;
    private NavigationDrawerController navigationController;
    private AlertDialog exitDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        String topicCode = getIntent().getStringExtra(EXTRA_TOPIC_CODE);
        String topicName = getIntent().getStringExtra(EXTRA_TOPIC_NAME);

        // Bind views
        textQuizProgress = findViewById(R.id.textQuizProgress);
        scoreBar = findViewById(R.id.scoreBar);
        textScoreCorrect = findViewById(R.id.textScoreCorrect);
        textScoreTotal = findViewById(R.id.textScoreTotal);
        progressQuiz = findViewById(R.id.progressQuiz);
        progressSubmit = findViewById(R.id.progressSubmit);
        textQuizError = findViewById(R.id.textQuizError);
        buttonQuizRetry = findViewById(R.id.buttonQuizRetry);
        cardQuestion = findViewById(R.id.cardQuestion);
        textQuizPrompt = findViewById(R.id.textQuizPrompt);
        optionsContainer = findViewById(R.id.optionsContainer);
        optionButtons = new MaterialButton[]{
                findViewById(R.id.buttonOption0),
                findViewById(R.id.buttonOption1),
                findViewById(R.id.buttonOption2),
                findViewById(R.id.buttonOption3),
        };
        cardResult = findViewById(R.id.cardResult);
        textResultVerdict = findViewById(R.id.textResultVerdict);
        textResultCorrectAnswer = findViewById(R.id.textResultCorrectAnswer);
        finishedContainer = findViewById(R.id.finishedContainer);
        textFinalTotal = findViewById(R.id.textFinalTotal);
        textFinalCorrect = findViewById(R.id.textFinalCorrect);
        textFinalWrong = findViewById(R.id.textFinalWrong);
        textFinalAccuracy = findViewById(R.id.textFinalAccuracy);
        buttonPlayAgain = findViewById(R.id.buttonPlayAgain);
        buttonQuizDone = findViewById(R.id.buttonQuizDone);
        buttonContinue = findViewById(R.id.buttonContinue);

        // Tiêu đề
        TextView textQuizTitle = findViewById(R.id.textQuizTitle);
        if (topicName != null && !topicName.trim().isEmpty()) {
            textQuizTitle.setText("Quiz: " + topicName);
        }

        buttonContinue.setOnClickListener(v -> viewModel.nextQuestion());
        buttonPlayAgain.setOnClickListener(v -> viewModel.playAgain());
        buttonQuizDone.setOnClickListener(v -> goHome());
        buttonQuizRetry.setOnClickListener(v -> viewModel.retry());

        // ViewModel
        viewModel = new ViewModelProvider(this).get(QuizViewModel.class);
        navigationController = NavigationDrawerController.attach(
                this,
                NavigationRoute.QUIZ,
                R.id.buttonQuizBack,
                textQuizTitle.getText(),
                this::handleQuizBack
        );

        // Observe
        viewModel.getState().observe(this, state -> render(state));
        viewModel.getCurrentIndex().observe(this, idx -> renderQuestion());
        viewModel.getLastAnswer().observe(this, answer -> renderResult(answer));

        // Bắt đầu quiz (ViewModel tự biết nếu đã có câu hỏi sau rotation)
        viewModel.initIfNeeded(topicCode);
    }

    private void render(QuizViewModel.QuizState state) {
        // Ẩn hết trước
        progressQuiz.setVisibility(View.GONE);
        textQuizError.setVisibility(View.GONE);
        buttonQuizRetry.setVisibility(View.GONE);
        cardQuestion.setVisibility(View.GONE);
        optionsContainer.setVisibility(View.GONE);
        progressSubmit.setVisibility(View.GONE);
        cardResult.setVisibility(View.GONE);
        finishedContainer.setVisibility(View.GONE);
        buttonContinue.setVisibility(View.GONE);
        scoreBar.setVisibility(View.GONE);
        textQuizProgress.setVisibility(View.INVISIBLE);

        switch (state) {
            case LOADING_QUESTIONS:
                progressQuiz.setVisibility(View.VISIBLE);
                break;

            case ERROR:
                textQuizError.setVisibility(View.VISIBLE);
                textQuizError.setText(viewModel.getErrorMessage().getValue());
                buttonQuizRetry.setVisibility(View.VISIBLE);
                break;

            case QUESTION:
                scoreBar.setVisibility(View.VISIBLE);
                textQuizProgress.setVisibility(View.VISIBLE);
                cardQuestion.setVisibility(View.VISIBLE);
                optionsContainer.setVisibility(View.VISIBLE);
                if (viewModel.getErrorMessage().getValue() != null) {
                    textQuizError.setText(viewModel.getErrorMessage().getValue());
                    textQuizError.setVisibility(View.VISIBLE);
                }
                renderQuestion();
                break;

            case SUBMITTING:
                scoreBar.setVisibility(View.VISIBLE);
                textQuizProgress.setVisibility(View.VISIBLE);
                cardQuestion.setVisibility(View.VISIBLE);
                optionsContainer.setVisibility(View.VISIBLE);
                for (MaterialButton button : optionButtons) button.setEnabled(false);
                progressSubmit.setVisibility(View.VISIBLE);
                break;

            case RESULT:
                scoreBar.setVisibility(View.VISIBLE);
                textQuizProgress.setVisibility(View.VISIBLE);
                cardQuestion.setVisibility(View.VISIBLE);
                cardResult.setVisibility(View.VISIBLE);
                buttonContinue.setVisibility(View.VISIBLE);
                updateScore();
                break;

            case FINISHED:
                finishedContainer.setVisibility(View.VISIBLE);
                QuizResultSummary summary = QuizResultSummary.from(
                        viewModel.getCorrectCount(), viewModel.getAnsweredCount());
                textFinalTotal.setText(String.valueOf(summary.getTotal()));
                textFinalCorrect.setText(String.valueOf(summary.getCorrect()));
                textFinalWrong.setText(String.valueOf(summary.getWrong()));
                textFinalAccuracy.setText(summary.getAccuracyPercent() + "%");
                break;
        }
    }

    private void renderQuestion() {
        QuizQuestion question = viewModel.getCurrentQuestion();
        if (question == null) return;

        int idx = viewModel.getCurrentIndex().getValue() == null ? 0 : viewModel.getCurrentIndex().getValue();
        textQuizProgress.setText(getString(R.string.quiz_question_progress, idx + 1,
                viewModel.getQuestionCount()));
        textQuizPrompt.setText(question.getPrompt());
        updateScore();

        List<QuizOption> options = question.getOptions();
        for (int i = 0; i < optionButtons.length; i++) {
            MaterialButton btn = optionButtons[i];
            if (i < options.size()) {
                QuizOption opt = options.get(i);
                btn.setText(opt.getText());
                btn.setEnabled(true);
                resetOptionStyle(btn);
                int selectedId = opt.getFlashcardId();
                btn.setOnClickListener(v -> viewModel.submitAnswer(selectedId));
                btn.setVisibility(View.VISIBLE);
            } else {
                btn.setVisibility(View.GONE);
            }
        }
    }

    private void renderResult(QuizAnswerResponse answer) {
        if (answer == null) {
            // Reset style tất cả nút khi sang câu mới
            for (MaterialButton btn : optionButtons) resetOptionStyle(btn);
            return;
        }
        boolean correct = answer.isCorrect();

        // Highlight nút đúng/sai
        QuizQuestion question = viewModel.getCurrentQuestion();
        if (question != null && question.getOptions() != null) {
            List<QuizOption> options = question.getOptions();
            for (int i = 0; i < optionButtons.length; i++) {
                if (i >= options.size()) continue;
                MaterialButton btn = optionButtons[i];
                btn.setEnabled(false);
                int optId = options.get(i).getFlashcardId();
                if (optId == answer.getFlashcardId()) {
                    // Đáp án đúng → xanh lá
                    btn.setStrokeColor(ContextCompat.getColorStateList(this, android.R.color.holo_green_dark));
                    btn.setTextColor(android.graphics.Color.parseColor("#86EFAC"));
                } else if (!correct && optId == answer.getSelectedFlashcardId()) {
                    // Đáp án người dùng chọn mà sai → đỏ
                    btn.setStrokeColor(ContextCompat.getColorStateList(this, android.R.color.holo_red_dark));
                    btn.setTextColor(android.graphics.Color.parseColor("#FDA4AF"));
                }
            }
        }

        // Card kết quả
        if (correct) {
            textResultVerdict.setText("✅ Đúng rồi!");
            textResultVerdict.setTextColor(android.graphics.Color.parseColor("#86EFAC"));
            cardResult.setStrokeColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            textResultVerdict.setText("❌ Sai rồi");
            textResultVerdict.setTextColor(android.graphics.Color.parseColor("#FDA4AF"));
            cardResult.setStrokeColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }
        textResultCorrectAnswer.setText(answer.getCorrectAnswer());
    }

    private void resetOptionStyle(MaterialButton btn) {
        btn.setEnabled(true);
        btn.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#526782")));
        btn.setTextColor(android.graphics.Color.parseColor("#E2E8F0"));
    }

    private void updateScore() {
        textScoreCorrect.setText(String.valueOf(viewModel.getCorrectCount()));
        textScoreTotal.setText(String.valueOf(viewModel.getAnsweredCount()));
    }

    private void handleQuizBack() {
        QuizViewModel.QuizState state = viewModel.getState().getValue();
        QuizBackPolicy.Action action = QuizBackPolicy.decide(state, viewModel.getAnsweredCount());
        if (action == QuizBackPolicy.Action.BLOCK_WHILE_SUBMITTING) {
            Toast.makeText(this, R.string.quiz_back_submitting, Toast.LENGTH_SHORT).show();
            return;
        }
        if (action == QuizBackPolicy.Action.LEAVE) {
            navigationController.performDefaultBack();
            return;
        }
        if (exitDialog != null && exitDialog.isShowing()) return;
        exitDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.quiz_back_title)
                .setMessage(R.string.quiz_back_message)
                .setNegativeButton(R.string.quiz_back_stay, null)
                .setPositiveButton(R.string.quiz_back_leave,
                        (dialog, which) -> navigationController.performDefaultBack())
                .create();
        exitDialog.show();
    }

    private void goHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
