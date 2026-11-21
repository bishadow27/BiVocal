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
import com.example.bivoca.ui.home.TrangChuActivity;
import com.example.bivoca.ui.navigation.NavigationDrawerController;
import com.example.bivoca.ui.navigation.NavigationRoute;
import com.example.bivoca.ui.navigation.NavigationRoutePolicy;
import com.example.bivoca.ui.navigation.LearningBottomNavigation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/**
 * Màn hình quiz.
 * Nhận topic code từ Intent (tùy chọn).
 * Điều hướng: TheTuVungActivity hoặc TrangChuActivity → TracNghiemActivity.
 */
public class TracNghiemActivity extends AppCompatActivity {

    public static final String EXTRA_TOPIC_CODE = "com.example.bivoca.QUIZ_TOPIC_CODE";
    public static final String EXTRA_TOPIC_NAME = "com.example.bivoca.QUIZ_TOPIC_NAME";

    private QuizViewModel viewModel;
    private QuizSessionScreen sessionScreenV2;

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
    private Runnable pendingNavigation;
    private String sessionKey;
    private Bundle sessionUi = new Bundle();
    private int restoredScroll;
    private static final String STATE_LEARNING_UI = "quiz_ui_by_session";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(com.example.bivoca.BuildConfig.QUIZ_SESSION_V2_ENABLED){
            setContentView(R.layout.activity_quiz_session);
            viewModel=new ViewModelProvider(this).get("quiz-session-v2",QuizViewModel.class);
            sessionScreenV2=new QuizSessionScreen(this,viewModel,getIntent().getStringExtra(EXTRA_TOPIC_CODE));
            return;
        }
        setContentView(R.layout.activity_trac_nghiem);

        String topicCode = getIntent().getStringExtra(EXTRA_TOPIC_CODE);
        String topicName = getIntent().getStringExtra(EXTRA_TOPIC_NAME);
        sessionKey = NavigationRoutePolicy.quizSessionKey(topicCode);
        if (savedInstanceState != null) {
            Bundle sessions = savedInstanceState.getBundle(STATE_LEARNING_UI);
            if (sessions != null) sessionUi = sessions;
            restoredScroll = sessionUi.getInt(sessionKey,0);
        }

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
        boolean[] created = {false};
        viewModel = new ViewModelProvider(this,new ViewModelProvider.Factory() {
            @Override public <T extends androidx.lifecycle.ViewModel> T create(
                    Class<T> type, androidx.lifecycle.viewmodel.CreationExtras extras) {
                created[0]=true;
                return TracNghiemActivity.this.getDefaultViewModelProviderFactory().create(type,extras);
            }
        }).get(sessionKey,QuizViewModel.class);
        navigationController = NavigationDrawerController.attach(
                this,
                NavigationRoute.QUIZ,
                R.id.buttonQuizBack,
                textQuizTitle.getText(),
                this::handleQuizBack
        );

        LearningBottomNavigation.bind(this,NavigationRoute.QUIZ,this::requestLearningNavigation);

        // Observe
        viewModel.getState().observe(this, state -> render(state));
        viewModel.getCurrentIndex().observe(this, idx -> renderQuestion());
        viewModel.getLastAnswer().observe(this, answer -> renderResult(answer));

        // Bắt đầu quiz (ViewModel tự biết nếu đã có câu hỏi sau rotation)
        if(created[0]) viewModel.initIfNeeded(topicCode);
        findViewById(R.id.quizContentScroll).post(() -> ((android.widget.ScrollView)findViewById(R.id.quizContentScroll)).scrollTo(0,restoredScroll));
    }

    private void render(QuizViewModel.QuizState state) {
        LearningBottomNavigation.setEnabled(this,state != QuizViewModel.QuizState.SUBMITTING);
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
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.bivocal_light_surface)));
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
                    btn.setStrokeColor(ContextCompat.getColorStateList(this, R.color.bivocal_light_mint_ink));
                    btn.setTextColor(getColor(R.color.bivocal_light_mint_ink));
                    btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.bivocal_light_mint_soft)));
                } else if (!correct && optId == answer.getSelectedFlashcardId()) {
                    // Đáp án người dùng chọn mà sai → đỏ
                    btn.setStrokeColor(ContextCompat.getColorStateList(this, R.color.bivocal_light_coral_ink));
                    btn.setTextColor(getColor(R.color.bivocal_light_coral_ink));
                    btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.bivocal_light_coral_soft)));
                }
            }
        }

        // Card kết quả
        if (correct) {
            textResultVerdict.setText("Đúng rồi!");
            cardResult.setCardBackgroundColor(getColor(R.color.bivocal_light_mint_soft));
            textResultVerdict.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.phase_d_status_check,0,0,0);
            textResultVerdict.setTextColor(getColor(R.color.bivocal_light_mint_ink));
            cardResult.setStrokeColor(ContextCompat.getColor(this, R.color.bivocal_light_mint_ink));
        } else {
            textResultVerdict.setText("Sai rồi");
            cardResult.setCardBackgroundColor(getColor(R.color.bivocal_light_coral_soft));
            textResultVerdict.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.phase_d_status_error,0,0,0);
            textResultVerdict.setTextColor(getColor(R.color.bivocal_light_coral_ink));
            cardResult.setStrokeColor(ContextCompat.getColor(this, R.color.bivocal_light_coral_ink));
        }
        textResultCorrectAnswer.setText(answer.getCorrectAnswer());
    }

    private void resetOptionStyle(MaterialButton btn) {
        btn.setEnabled(true);
        btn.setStrokeColor(android.content.res.ColorStateList.valueOf(getColor(R.color.bivocal_light_border)));
        btn.setTextColor(getColor(R.color.bivocal_light_text));
    }

    private void updateScore() {
        textScoreCorrect.setText(String.valueOf(viewModel.getCorrectCount()));
        textScoreTotal.setText(String.valueOf(viewModel.getAnsweredCount()));
    }

    private void handleQuizBack() {
        requestLearningNavigation(() -> navigationController.performDefaultBack());
    }

    private void requestLearningNavigation(Runnable destination) {
        QuizBackPolicy.Action action = QuizBackPolicy.decide(viewModel.getState().getValue(),viewModel.getAnsweredCount());
        if (action == QuizBackPolicy.Action.BLOCK_WHILE_SUBMITTING) {
            Toast.makeText(this,R.string.quiz_back_submitting,Toast.LENGTH_SHORT).show();return;
        }
        if (action == QuizBackPolicy.Action.LEAVE) { destination.run();return; }
        if (exitDialog != null && exitDialog.isShowing()) return;
        pendingNavigation = destination;
        exitDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.quiz_back_title).setMessage(R.string.quiz_back_message)
                .setNegativeButton(R.string.quiz_back_stay,(dialog,which)->pendingNavigation=null)
                .setOnCancelListener(dialog->pendingNavigation=null)
                .setPositiveButton(R.string.quiz_back_leave,(dialog,which)->{
                    Runnable requested=pendingNavigation;pendingNavigation=null;
                    if (QuizBackPolicy.decide(viewModel.getState().getValue(),viewModel.getAnsweredCount())
                            == QuizBackPolicy.Action.BLOCK_WHILE_SUBMITTING) {
                        Toast.makeText(this,R.string.quiz_back_submitting,Toast.LENGTH_SHORT).show();
                    } else if(requested!=null)requested.run();
                }).create();
        exitDialog.show();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(sessionScreenV2!=null){setIntent(intent);sessionScreenV2.receiveTopic(intent.getStringExtra(EXTRA_TOPIC_CODE),intent.getStringExtra(EXTRA_TOPIC_NAME));return;}
        // A tab/Drawer re-entry resumes the current quiz, not a new all-topics request.
        if (!intent.hasExtra(EXTRA_TOPIC_CODE)) return;
        String nextKey=NavigationRoutePolicy.quizSessionKey(intent.getStringExtra(EXTRA_TOPIC_CODE));
        if (sessionKey.equals(nextKey)) return;
        requestLearningNavigation(()->{
            sessionUi.putInt(sessionKey,findViewById(R.id.quizContentScroll).getScrollY());
            setIntent(intent);recreate();
        });
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        if(sessionScreenV2!=null){super.onSaveInstanceState(outState);return;}
        sessionUi.putInt(sessionKey,findViewById(R.id.quizContentScroll).getScrollY());
        outState.putBundle(STATE_LEARNING_UI,sessionUi);
        super.onSaveInstanceState(outState);
    }

    @Override protected void onDestroy() {
        if(sessionScreenV2!=null)sessionScreenV2.destroy();
        pendingNavigation=null;
        if(exitDialog!=null)exitDialog.dismiss();
        super.onDestroy();
    }

    @androidx.annotation.VisibleForTesting public QuizViewModel getLearningViewModel() { return viewModel; }

    private void goHome() {
        LearningBottomNavigation.navigate(this,NavigationRoute.HOME);
    }
}
