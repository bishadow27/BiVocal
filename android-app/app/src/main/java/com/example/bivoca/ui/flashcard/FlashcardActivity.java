package com.example.bivoca.ui.flashcard;

import android.content.Intent;
import android.animation.ValueAnimator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.AbstractSavedStateViewModelFactory;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.bivoca.R;
import com.example.bivoca.data.model.Flashcard;
import com.example.bivoca.data.model.ProgressDto;
import com.example.bivoca.data.repository.FlashcardRepository;
import com.example.bivoca.data.repository.ProgressRepository;
import com.example.bivoca.data.repository.LearningRepository;
import com.example.bivoca.ui.quiz.QuizActivity;
import com.example.bivoca.ui.navigation.NavigationDrawerController;
import com.example.bivoca.ui.navigation.NavigationRoute;
import com.example.bivoca.ui.navigation.NavigationRoutePolicy;
import com.example.bivoca.ui.navigation.LearningBottomNavigation;
import com.example.bivoca.utils.SessionManager;
import com.example.bivoca.settings.SettingsPreferenceManager;

import java.util.List;
import java.util.Locale;

public class FlashcardActivity extends AppCompatActivity {
    public static final String EXTRA_TOPIC_CODE = "com.example.bivoca.TOPIC_CODE";
    public static final String EXTRA_TOPIC_NAME = "com.example.bivoca.TOPIC_NAME";
    public static final String EXTRA_REVIEW_TODAY = "com.example.bivoca.REVIEW_TODAY";
    public static final String EXTRA_NEW_TODAY = "com.example.bivoca.NEW_TODAY";
    public static final String EXTRA_NEW_LIMIT = "com.example.bivoca.NEW_LIMIT";
    private static final String STATE_FLIPPED_CARD = "flipped_card";
    private static final String STATE_BROWSE_ACK = "phase_d_browse_ack";
    private static final String STATE_AUTO_SPOKEN_CARD = "auto_spoken_card";

    private static final String STATE_LEARNING_UI = "learning_ui_by_session";
    private static final String STATE_LEARNING_KEY = "learning_session_key";
    private static final String STATE_BROWSE_FAILURE = "learning_browse_failure";
    private Bundle sessionUi = new Bundle();
    private String sessionKey;
    private int restoredPageScroll, restoredInnerScroll;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private FlashcardViewModel viewModel;
    private String topicCode;
    private TextView position, english, pronunciation, vietnamese, metadata;
    private TextView example, exampleVi, error, speechStatus;
    private TextView reviewError;
    private TextView backPronunciation, backMetadata, frontHint, backHint, backLabel;
    private View progress, empty, card, examples, controls;
    private View layoutCardFront, layoutCardBack, reviewSection;
    private TextView textCardSideBadge, buttonFavorite;
    private Button previous, next, speak, retry, buttonNotRemember, buttonRemembered;
    private NavigationDrawerController drawerController;
    private TextToSpeech tts;
    private boolean ttsReady;
    private boolean destroyed;
    private int displayedCardId = -1;
    private boolean isCardFlipped = false;
    private boolean isCurrentFavorite = false;
    private boolean favoritePending;
    private boolean reviewPending;
    private boolean browseReviewAcknowledged;
    @Nullable private String browseReviewFailure;
    private int cardVersion;
    private int favoriteVersion;
    private View buttonStartQuiz;
    private ProgressRepository progressRepository;
    private boolean dueMode;
    private boolean newMode;
    private int newLimit;
    private int sessionOwnerId;
    private int restoredFlippedCardId = -1;
    @Nullable private String activeUtterance;
    private SettingsPreferenceManager learningSettings;
    private int autoSpokenCardId = -1;
    private final Runnable resizeViewportTask = this::resizeCardViewport;
    private final android.view.ViewTreeObserver.OnGlobalLayoutListener viewportLayoutListener =
            this::scheduleCardViewport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard);
        findViewById(R.id.flashcardPageScroll).getViewTreeObserver()
                .addOnGlobalLayoutListener(viewportLayoutListener);
        progressRepository = new ProgressRepository(this);
        dueMode = getIntent().getBooleanExtra(EXTRA_REVIEW_TODAY, false);
        newMode = getIntent().getBooleanExtra(EXTRA_NEW_TODAY, false);
        newLimit = Math.max(1, Math.min(20, getIntent().getIntExtra(EXTRA_NEW_LIMIT, 10)));
        sessionOwnerId = new SessionManager(this).getUserId();
        learningSettings = new SettingsPreferenceManager(this);
        topicCode = getIntent().getStringExtra(EXTRA_TOPIC_CODE);
        sessionKey = learningSessionKey(getIntent());
        if (savedInstanceState != null) {
            Bundle sessions = savedInstanceState.getBundle(STATE_LEARNING_UI);
            if (sessions != null) sessionUi = sessions;
            Bundle ui = sessionUi.getBundle(sessionKey);
            if (ui == null && (savedInstanceState.getString(STATE_LEARNING_KEY) == null
                    || sessionKey.equals(savedInstanceState.getString(STATE_LEARNING_KEY)))) ui = savedInstanceState;
            if (ui != null) {
                browseReviewAcknowledged = ui.getBoolean(STATE_BROWSE_ACK, false);
                browseReviewFailure = ui.getString(STATE_BROWSE_FAILURE);
                restoredFlippedCardId = ui.getInt(STATE_FLIPPED_CARD, -1);
                autoSpokenCardId = ui.getInt(STATE_AUTO_SPOKEN_CARD, -1);
                restoredPageScroll = ui.getInt("page_scroll", 0);
                restoredInnerScroll = ui.getInt("inner_scroll", 0);
            }
        }

        String topicName = getIntent().getStringExtra(EXTRA_TOPIC_NAME);
        TextView title = findViewById(R.id.textTopicTitle);
        title.setText(dueMode ? getString(R.string.review_due_title) : newMode ? getString(R.string.fc16_new_title) : hasText(topicName) ? topicName :
                (FlashcardRepository.isValidTopicCode(topicCode) ? topicCode : getString(R.string.flashcard_title)));

        position = findViewById(R.id.textPosition);
        english = findViewById(R.id.textEnglish);
        pronunciation = findViewById(R.id.textPronunciation);
        vietnamese = findViewById(R.id.textVietnamese);
        metadata = findViewById(R.id.textMetadata);
        example = findViewById(R.id.textExample);
        exampleVi = findViewById(R.id.textExampleVi);
        error = findViewById(R.id.textFlashcardError);
        speechStatus = findViewById(R.id.textSpeechStatus);
        reviewError = findViewById(R.id.textReviewError);
        backPronunciation = findViewById(R.id.textBackPronunciation);
        backMetadata = findViewById(R.id.textBackMetadata);
        frontHint = findViewById(R.id.textCardFrontHint);
        backHint = findViewById(R.id.textCardBackHint);
        backLabel = findViewById(R.id.textCardBackLabel);
        progress = findViewById(R.id.progressFlashcards);
        empty = findViewById(R.id.textFlashcardEmpty);
        card = findViewById(R.id.flashcardContent);
        examples = findViewById(R.id.exampleSection);
        controls = findViewById(R.id.flashcardControls);
        previous = findViewById(R.id.buttonPrevious);
        next = findViewById(R.id.buttonNext);
        speak = findViewById(R.id.buttonSpeak);
        retry = findViewById(R.id.buttonRetryFlashcards);

        layoutCardFront = findViewById(R.id.layoutCardFront);
        layoutCardBack = findViewById(R.id.layoutCardBack);
        textCardSideBadge = findViewById(R.id.textCardSideBadge);
        buttonFavorite = findViewById(R.id.buttonFavorite);
        reviewSection = findViewById(R.id.reviewSection);
        buttonNotRemember = findViewById(R.id.buttonNotRemember);
        buttonRemembered = findViewById(R.id.buttonRemembered);

        viewModel = new ViewModelProvider(this, new AbstractSavedStateViewModelFactory(this, null) {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            protected <T extends ViewModel> T create(@NonNull String key,
                    @NonNull Class<T> modelClass, @NonNull SavedStateHandle handle) {
                if (!modelClass.isAssignableFrom(FlashcardViewModel.class)) {
                    throw new IllegalArgumentException("Unknown ViewModel class");
                }
                return (T) new FlashcardViewModel(handle,
                        new FlashcardRepository(getApplicationContext()), progressRepository,
                        new LearningRepository(getApplicationContext()));
            }
        }).get(sessionKey, FlashcardViewModel.class);
        drawerController = NavigationDrawerController.attachSession(this,
                dueMode ? NavigationRoute.REVIEW_TODAY : newMode ? NavigationRoute.NEW_TODAY : NavigationRoute.FLASHCARDS,
                R.id.buttonFlashcardBack,
                () -> {
                    if (reviewPending || Boolean.TRUE.equals(viewModel.getReviewPending().getValue()))
                        Toast.makeText(this,R.string.fc16_pending,Toast.LENGTH_SHORT).show();
                    else drawerController.performDefaultBack();
                });
        LearningBottomNavigation.bind(this, dueMode ? NavigationRoute.REVIEW_TODAY
                : newMode ? NavigationRoute.NEW_TODAY : NavigationRoute.FLASHCARDS, this::requestLearningNavigation);
        ((TextView)findViewById(R.id.textSessionMode)).setText(dueMode?R.string.fc16_review:newMode?R.string.fc16_new:R.string.fc16_browse);
        ((TextView)findViewById(R.id.textSessionProgressLabel)).setText(dueMode||newMode?R.string.fc16_progress:R.string.fc16_browse_progress);
        findViewById(R.id.buttonFlashcardMenu).setOnClickListener(this::showFlashcardMenu);
        findViewById(R.id.buttonFlipCard).setOnClickListener(v -> toggleCardFlip());
        findViewById(R.id.buttonSpeakBack).setOnClickListener(v -> speakCurrentWord());
        findViewById(R.id.buttonSpeakExample).setOnClickListener(v -> {
            List<Flashcard> items=viewModel.getFlashcards().getValue();Integer index=viewModel.getCurrentIndex().getValue();
            if(items!=null&&index!=null&&index>=0&&index<items.size())speakText(items.get(index).getExample());
        });
        findViewById(R.id.buttonSessionContinue).setOnClickListener(v -> {
            if(dueMode)viewModel.loadDueCards();else if(newMode)viewModel.loadNewCards(newLimit);
        });
        ((TextView)findViewById(R.id.buttonSessionContinue)).setText(dueMode?R.string.fc16_review_more:R.string.fc16_learn_more);
        findViewById(R.id.buttonSessionTopics).setOnClickListener(v -> {
            Intent intent=new Intent(this,com.example.bivoca.ui.topics.TopicsActivity.class);
            LearningBottomNavigation.open(this,intent);
        });
        buttonStartQuiz = findViewById(R.id.buttonStartQuiz);
        if (buttonStartQuiz != null) {
            if (dueMode || newMode) buttonStartQuiz.setVisibility(View.GONE);
            buttonStartQuiz.setOnClickListener(view -> {
                if (!canUseOnlineActions()) return;
                Intent intent = new Intent(FlashcardActivity.this, QuizActivity.class);
                intent.putExtra(QuizActivity.EXTRA_TOPIC_CODE, topicCode);
                intent.putExtra(QuizActivity.EXTRA_TOPIC_NAME, topicName);
                LearningBottomNavigation.open(this,intent);
            });
        }
        if (dueMode || newMode) {
            if (buttonFavorite != null) buttonFavorite.setVisibility(View.GONE);
            previous.setVisibility(View.GONE);
            next.setVisibility(View.GONE);
        }
        previous.setOnClickListener(view -> viewModel.previous());
        next.setOnClickListener(view -> viewModel.next());
        speak.setOnClickListener(view -> speakCurrentWord());
        retry.setOnClickListener(view -> {
            if (dueMode) viewModel.loadDueCards();
            else if (newMode) viewModel.loadNewCards(newLimit);
            else viewModel.loadFlashcards(topicCode);
        });

        if (card != null) {
            card.setOnClickListener(view -> toggleCardFlip());
            layoutCardFront.setOnClickListener(view -> toggleCardFlip());
            layoutCardBack.setOnClickListener(view -> toggleCardFlip());
        }
        if (buttonFavorite != null) {
            buttonFavorite.setOnClickListener(view -> toggleFavorite());
        }
        if (buttonNotRemember != null) {
            buttonNotRemember.setOnClickListener(view -> submitReview(false));
        }
        if (buttonRemembered != null) {
            buttonRemembered.setOnClickListener(view -> submitReview(true));
        }

        viewModel.getFlashcards().observe(this, value -> render());
        viewModel.getLoading().observe(this, value -> render());
        viewModel.getError().observe(this, value -> render());
        viewModel.getCurrentIndex().observe(this, value -> render());
        viewModel.getReviewPending().observe(this, value -> render());
        viewModel.getReviewCompleted().observe(this, value -> render());
        viewModel.getReviewError().observe(this, value -> render());
        viewModel.getSessionTotal().observe(this, value -> render());
        viewModel.getSessionProcessed().observe(this, value -> render());

        initializeSpeech();
        if (dueMode) viewModel.ensureDueLoaded();
        else if (newMode) viewModel.ensureNewLoaded(newLimit);
        else viewModel.ensureFlashcardsLoaded(topicCode);
        findViewById(R.id.flashcardPageScroll).post(() -> {
            ((androidx.core.widget.NestedScrollView)findViewById(R.id.flashcardPageScroll)).scrollTo(0, restoredPageScroll);
            ((android.widget.ScrollView)findViewById(R.id.flashcardInnerScroll)).scrollTo(0, restoredInnerScroll);
        });
    }

    private static String learningSessionKey(Intent intent) {
        return NavigationRoutePolicy.flashcardSessionKey(intent.getBooleanExtra(EXTRA_REVIEW_TODAY,false),
                intent.getBooleanExtra(EXTRA_NEW_TODAY,false),intent.getStringExtra(EXTRA_TOPIC_CODE),
                intent.getIntExtra(EXTRA_NEW_LIMIT,10));
    }

    private void requestLearningNavigation(Runnable destination) {
        if (reviewPending || Boolean.TRUE.equals(viewModel.getReviewPending().getValue()))
            Toast.makeText(this,R.string.fc16_pending,Toast.LENGTH_SHORT).show();
        else destination.run();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (sessionKey.equals(learningSessionKey(intent))) {
            if (newMode) {
                newLimit = Math.max(1,Math.min(20,intent.getIntExtra(EXTRA_NEW_LIMIT,10)));
                setIntent(intent);
            }
            return;
        }
        requestLearningNavigation(() -> {
            sessionUi.putBundle(sessionKey, captureLearningUi());
            setIntent(intent);
            recreate();
        });
    }

    private Bundle captureLearningUi() {
        Bundle ui = new Bundle();
        ui.putBoolean(STATE_BROWSE_ACK,browseReviewAcknowledged);
        ui.putString(STATE_BROWSE_FAILURE,browseReviewFailure);
        ui.putInt(STATE_FLIPPED_CARD,isCardFlipped?displayedCardId:-1);
        ui.putInt(STATE_AUTO_SPOKEN_CARD,autoSpokenCardId);
        ui.putInt("page_scroll",findViewById(R.id.flashcardPageScroll).getScrollY());
        ui.putInt("inner_scroll",findViewById(R.id.flashcardInnerScroll).getScrollY());
        return ui;
    }

    @androidx.annotation.VisibleForTesting public FlashcardViewModel getLearningViewModel() { return viewModel; }

    private void toggleCardFlip() {
        if (card == null || reviewPending || Boolean.TRUE.equals(viewModel.getReviewPending().getValue())) return;
        if (learningSettings.isReduceMotionEnabled()
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ValueAnimator.areAnimatorsEnabled())) {
            isCardFlipped = !isCardFlipped;
            updateCardFlipUI();
            return;
        }
        card.animate().scaleX(0.94f).alpha(0.65f).setDuration(90).withEndAction(() -> {
            isCardFlipped = !isCardFlipped;
            updateCardFlipUI();
            card.animate().scaleX(1f).alpha(1f).setDuration(120).start();
        }).start();
    }

    private void updateCardFlipUI() {
        if (layoutCardFront != null && layoutCardBack != null) {
            layoutCardFront.setVisibility(isCardFlipped ? View.GONE : View.VISIBLE);
            layoutCardBack.setVisibility(isCardFlipped ? View.VISIBLE : View.GONE);
        }
        if (textCardSideBadge != null) {
            boolean vietnameseFirst = SettingsPreferenceManager.FRONT_VIETNAMESE.equals(
                    learningSettings.getFlashcardFront());
            textCardSideBadge.setText(isCardFlipped
                    ? (vietnameseFirst ? "Từ vựng & Ví dụ" : "Nghĩa & Ví dụ")
                    : (vietnameseFirst ? "Tiếng Việt" : "Từ vựng"));
        }
        if (reviewSection != null) {
            reviewSection.setVisibility(isCardFlipped ? View.VISIBLE : View.GONE);
        }
        ((TextView)findViewById(R.id.buttonFlipCard)).setText(isCardFlipped?R.string.fc16_front:R.string.fc16_flip);
        applyFlipAppearance((com.google.android.material.button.MaterialButton)findViewById(R.id.buttonFlipCard), isCardFlipped);
        int navigationVisibility = dueMode || newMode ? View.GONE
                : isCardFlipped ? View.INVISIBLE : View.VISIBLE;
        previous.setVisibility(navigationVisibility);
        next.setVisibility(navigationVisibility);
        ((android.widget.ScrollView)findViewById(R.id.flashcardInnerScroll)).scrollTo(0,0);
    }

    private void scheduleCardViewport() {
        View page = findViewById(R.id.flashcardPageScroll);
        page.removeCallbacks(resizeViewportTask);
        page.post(resizeViewportTask);
    }

    /** Both faces share the same card height and flip action position. */
    private void resizeCardViewport() {
        if (destroyed) return;
        View page = findViewById(R.id.flashcardPageScroll);
        View viewport = findViewById(R.id.flashcardViewport);
        int available = page.getHeight() - page.getPaddingTop() - page.getPaddingBottom();
        if (available <= 0) return;
        android.widget.LinearLayout.LayoutParams params =
                (android.widget.LinearLayout.LayoutParams) viewport.getLayoutParams();
        int target;
        if (controls.getVisibility() == View.VISIBLE) {
            target = Math.round(available * 0.80f) - viewport.getTop() - params.bottomMargin
                    - controls.getTop() - controls.getHeight() / 2;
        } else {
            target = available - viewport.getTop() - params.bottomMargin
                    - ((View) controls.getParent()).getHeight();
        }
        int preferredMinimum = getResources().getDimensionPixelSize(R.dimen.bivocal_flashcard_preferred_min_height);
        int compactMinimum = getResources().getDimensionPixelSize(R.dimen.bivocal_flashcard_compact_min_height);
        int roomForFlip = available - viewport.getTop() - params.bottomMargin
                - controls.getTop() - controls.getHeight();
        // Large-font compact screens keep the flip reachable; card content still scrolls inside.
        int minimum = controls.getVisibility() == View.VISIBLE
                ? Math.min(preferredMinimum, Math.max(compactMinimum, roomForFlip)) : preferredMinimum;
        target = Math.max(minimum, target);
        if (params.height != target) {
            params.height = target;
            viewport.setLayoutParams(params);
        }
    }

    /** Presentation only: successful callbacks / acknowledged queue counters supply the flag. */
    private static void bindReviewFeedback(TextView status, boolean visible, boolean saving,
            @Nullable String failure, boolean acknowledged) {
        boolean failed = hasText(failure);
        status.setVisibility(visible && (saving || failed || acknowledged) ? View.VISIBLE : View.GONE);
        status.setText(saving ? status.getContext().getString(R.string.fc16_pending) : failed ? failure
                : acknowledged ? status.getContext().getString(R.string.phase_d_ack) : null);
        status.setTextColor(status.getContext().getColor(saving ? R.color.bivocal_light_cobalt : failed ? R.color.bivocal_light_coral_ink : R.color.bivocal_light_mint_ink));
        status.setCompoundDrawablesRelativeWithIntrinsicBounds(saving ? 0 : failed ? R.drawable.phase_d_status_error
                : acknowledged ? R.drawable.phase_d_status_check : 0, 0, 0, 0);
        status.setCompoundDrawablePadding(Math.round(8 * status.getResources().getDisplayMetrics().density));
    }

    private static void applyFlipAppearance(com.google.android.material.button.MaterialButton button, boolean back) {
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(button.getContext().getColor(back ? R.color.bivocal_light_surface : R.color.bivocal_light_cobalt)));
        button.setTextColor(button.getContext().getColor(back ? R.color.bivocal_light_text : R.color.bivocal_light_on_primary));
        button.setStrokeColor(android.content.res.ColorStateList.valueOf(button.getContext().getColor(R.color.bivocal_light_border)));
        button.setStrokeWidth(back ? Math.max(1, Math.round(button.getResources().getDisplayMetrics().density)) : 0);
    }

    private void showFlashcardMenu(View anchor) {
        androidx.appcompat.widget.PopupMenu menu=new androidx.appcompat.widget.PopupMenu(this,anchor);
        menu.getMenu().add(0,1,0,R.string.nav_open_description);
        if(!dueMode&&!newMode) {
            menu.getMenu().add(0,2,1,isCurrentFavorite?R.string.fc16_unfavorite:R.string.fc16_favorite).setEnabled(buttonFavorite.isEnabled());
            menu.getMenu().add(0,3,2,R.string.fc16_quiz).setEnabled(buttonStartQuiz.isEnabled());
        }
        menu.setOnMenuItemClickListener(item->{
            if(item.getItemId()==1)drawerController.open();
            else if(item.getItemId()==2)buttonFavorite.performClick();
            else if(item.getItemId()==3)buttonStartQuiz.performClick();
            return true;
        });menu.show();
    }

    private void toggleFavorite() {
        if (!canUseOnlineActions() || favoritePending) return;
        List<Flashcard> cards = viewModel.getFlashcards().getValue();
        Integer index = viewModel.getCurrentIndex().getValue();
        if (cards == null || index == null || index < 0 || index >= cards.size()) return;
        Flashcard current = cards.get(index);
        final int currentId = current.getId();
        final int version = cardVersion;
        favoriteVersion++;
        favoritePending = true;
        boolean targetState = !isCurrentFavorite;
        isCurrentFavorite = targetState;
        if (buttonFavorite != null) {
            buttonFavorite.setText(isCurrentFavorite ? "❤️" : "🤍");
            buttonFavorite.setEnabled(false);
        }
        progressRepository.toggleFavorite(currentId, targetState, new ProgressRepository.ProgressCallback() {
            @Override
            public void onSuccess(ProgressDto progress) {
                if (!isCurrentOnlineCard(currentId, version)) return;
                favoritePending = false;
                isCurrentFavorite = progress.isFavorite();
                if (buttonFavorite != null) {
                    buttonFavorite.setText(isCurrentFavorite ? "❤️" : "🤍");
                }
                render();
            }

            @Override
            public void onError(String message) {
                if (!isCurrentOnlineCard(currentId, version)) return;
                favoritePending = false;
                isCurrentFavorite = !targetState;
                if (buttonFavorite != null) {
                    buttonFavorite.setText(isCurrentFavorite ? "❤️" : "🤍");
                }
                Toast.makeText(FlashcardActivity.this, message, Toast.LENGTH_LONG).show();
                render();
            }
        });
    }

    private void submitReview(boolean correct) {
        if (dueMode || newMode) {
            if (canUseOnlineActions()) viewModel.submitDueReview(correct);
            return;
        }
        if (!canUseOnlineActions() || reviewPending) return;
        List<Flashcard> cards = viewModel.getFlashcards().getValue();
        Integer index = viewModel.getCurrentIndex().getValue();
        if (cards == null || index == null || index < 0 || index >= cards.size()) return;
        Flashcard current = cards.get(index);
        final int currentId = current.getId();
        final int version = cardVersion;
        browseReviewAcknowledged = false;
        browseReviewFailure = null;
        reviewPending = true;
        if (buttonNotRemember != null) buttonNotRemember.setEnabled(false);
        if (buttonRemembered != null) buttonRemembered.setEnabled(false);
        render();
        progressRepository.submitReview(current.getId(), correct, new ProgressRepository.ProgressCallback() {
            @Override
            public void onSuccess(ProgressDto progress) {
                if (!isCurrentOnlineCard(currentId, version)) return;
                reviewPending = false;
                browseReviewAcknowledged = true;
                browseReviewFailure = null;
                if (index < cards.size() - 1) {
                    viewModel.next();
                }
                render();
            }

            @Override
            public void onError(String message) {
                if (!isCurrentOnlineCard(currentId, version)) return;
                reviewPending = false;
                browseReviewFailure = hasText(message) ? message : getString(R.string.phase_d_save_failed);
                Toast.makeText(FlashcardActivity.this, message, Toast.LENGTH_LONG).show();
                render();
            }
        });
    }

    private void render() {
        boolean loading = Boolean.TRUE.equals(viewModel.getLoading().getValue());
        String message = viewModel.getError().getValue();
        List<Flashcard> cards = viewModel.getFlashcards().getValue();
        boolean failed = !loading && hasText(message);
        boolean available = !loading && !failed && cards != null && !cards.isEmpty();
        boolean duePending = Boolean.TRUE.equals(viewModel.getReviewPending().getValue());
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        error.setVisibility(failed ? View.VISIBLE : View.GONE);
        error.setText(message);
        retry.setVisibility(!loading && !duePending && failed
                && (dueMode || newMode || FlashcardRepository.isValidTopicCode(topicCode))
                ? View.VISIBLE : View.GONE);
        retry.setText(R.string.online_retry);
        int sessionTotal = viewModel.getSessionTotal().getValue();
        int processed = viewModel.getSessionProcessed().getValue();
        boolean completed = (dueMode || newMode) && !loading && !failed && sessionTotal > 0
                && Boolean.TRUE.equals(viewModel.getReviewCompleted().getValue())
                && cards != null && cards.isEmpty();
        findViewById(R.id.flashcardComplete).setVisibility(completed ? View.VISIBLE : View.GONE);
        ((TextView)findViewById(R.id.textSessionProcessed)).setText(String.valueOf(processed));
        ((TextView)findViewById(R.id.textSessionFinalProgress)).setText(getString(R.string.fc16_fraction,processed,sessionTotal));
        int progressTotal = dueMode || newMode ? sessionTotal : viewModel.getTopicTotal();
        int progressValue = dueMode || newMode ? processed : available ? viewModel.getGlobalPosition() : 0;
        ((TextView)findViewById(R.id.textSessionProgress)).setText(getString(R.string.fc16_fraction,progressValue,progressTotal));
        ((android.widget.ProgressBar)findViewById(R.id.progressSession)).setProgress(progressTotal > 0
                ? (int)(100L * progressValue / progressTotal) : 0);
        empty.setVisibility(!completed && !loading && !failed && cards != null && cards.isEmpty() ? View.VISIBLE : View.GONE);
        if ((dueMode || newMode) && cards != null && cards.isEmpty()) {
            ((TextView) empty).setText(Boolean.TRUE.equals(viewModel.getReviewCompleted().getValue())
                    ? (newMode ? "Đã hoàn tất danh sách từ mới hôm nay." : getString(R.string.review_due_complete))
                    : (newMode ? "Không còn từ mới phù hợp để học." : getString(R.string.review_due_empty)));
        }
        boolean saving = duePending || reviewPending;
        LearningBottomNavigation.setEnabled(this,!saving);
        bindReviewFeedback(reviewError, available || completed, saving,
                dueMode || newMode ? viewModel.getReviewError().getValue() : browseReviewFailure,
                dueMode || newMode ? processed > 0 : browseReviewAcknowledged);
        View menu = findViewById(R.id.buttonFlashcardMenu);
        menu.setEnabled(!saving); menu.setAlpha(saving ? 0.4f : 1f);
        card.setVisibility(available ? View.VISIBLE : View.GONE);
        controls.setVisibility(available ? View.VISIBLE : View.GONE);
        position.setVisibility(available ? View.VISIBLE : View.GONE);
        speechStatus.setVisibility(available ? View.VISIBLE : View.GONE);
        if (reviewSection != null && !available) reviewSection.setVisibility(View.GONE);
        previous.setEnabled(false);
        next.setEnabled(false);
        speak.setEnabled(false);
        findViewById(R.id.buttonSpeakBack).setEnabled(false);
        findViewById(R.id.buttonSpeakExample).setEnabled(false);
        findViewById(R.id.buttonFlipCard).setEnabled(available && !saving);
        findViewById(R.id.buttonFlipCard).setAlpha(available && !saving ? 1f : 0.4f);
        setOnlineActionAvailability(available && !duePending);
        if (!available) {
            stopSpeech();
            if (displayedCardId != -1) cardVersion++;
            displayedCardId = -1;
            favoritePending = false;
            reviewPending = false;
            return;
        }

        Integer selected = viewModel.getCurrentIndex().getValue();
        int index = Math.max(0, Math.min(selected == null ? 0 : selected, cards.size() - 1));
        Flashcard current = cards.get(index);
        if (displayedCardId != current.getId()) {
            browseReviewFailure = null;
            if (!dueMode && !newMode) bindReviewFeedback(reviewError, true, saving, null, browseReviewAcknowledged);
            stopSpeech();
            cardVersion++;
            favoritePending = false;
            reviewPending = false;
            displayedCardId = current.getId();
            isCardFlipped = restoredFlippedCardId == current.getId();
            restoredFlippedCardId = -1;
            updateCardFlipUI();
            isCurrentFavorite = false;
            if (buttonFavorite != null) buttonFavorite.setText("🤍");
            if (!dueMode && !newMode) loadCardProgress(current.getId());
            if (ttsReady) speechStatus.setText(R.string.flashcard_speech_ready);
        }
        int total = dueMode || newMode ? sessionTotal : viewModel.getTopicTotal();
        int currentPosition = dueMode || newMode ? processed + 1 : viewModel.getGlobalPosition();
        position.setText(getString(R.string.fc16_counter, currentPosition, total));
        boolean vietnameseFirst = SettingsPreferenceManager.FRONT_VIETNAMESE.equals(
                learningSettings.getFlashcardFront());
        english.setText(vietnameseFirst ? current.getVietnamese() : current.getEnglish());
        vietnamese.setText(vietnameseFirst ? current.getEnglish() : current.getVietnamese());
        String info = hasText(current.getPartOfSpeech()) ? current.getPartOfSpeech() : "";
        if (hasText(current.getLevel())) info += (info.isEmpty() ? "" : " · ") + current.getLevel();
        if (hasText(current.getContentStatus())) {
            String status = current.isImportedUnverified() ? getString(R.string.phase_d_unverified)
                    : "SEMANTIC_VERIFIED".equals(current.getContentStatus()) ? getString(R.string.phase_d_verified)
                    : current.getContentStatus();
            info += (info.isEmpty() ? "" : " · ") + status;
        }
        if (dueMode) info += (info.isEmpty() ? "" : " · ") + getString(R.string.review_memory_level, current.getMemoryLevel());
        optionalText(pronunciation, vietnameseFirst ? null : current.getPronunciation());
        optionalText(metadata, vietnameseFirst ? null : info);
        optionalText((TextView)findViewById(R.id.textBackWord), vietnameseFirst ? null : current.getEnglish());
        optionalText(backPronunciation, current.getPronunciation());
        optionalText(backMetadata, info);
        findViewById(R.id.textBackMetadataLabel).setVisibility(hasText(info) ? View.VISIBLE : View.GONE);
        findViewById(R.id.flashcardMetadataColumn).setVisibility(hasText(info) ? View.VISIBLE : View.GONE);
        backLabel.setText(vietnameseFirst ? "TỪ TIẾNG ANH" : "NGHĨA TIẾNG VIỆT");
        frontHint.setText(vietnameseFirst ? "Chạm vào thẻ để xem từ tiếng Anh" : "Chạm vào thẻ để xem nghĩa");
        backHint.setText(vietnameseFirst ? "Chạm vào thẻ để xem lại nghĩa" : "Chạm vào thẻ để lật lại từ vựng");
        optionalText(example, current.getExample());
        optionalText(exampleVi, current.getExampleVi());
        examples.setVisibility(hasText(current.getExample()) || hasText(current.getExampleVi()) ? View.VISIBLE : View.GONE);
        previous.setEnabled(!dueMode && !newMode && !reviewPending && viewModel.canPrevious());
        next.setEnabled(!dueMode && !newMode && !reviewPending && viewModel.canNext());
        previous.setAlpha(previous.isEnabled() ? 1f : 0.4f);
        next.setAlpha(next.isEnabled() ? 1f : 0.4f);
        speak.setEnabled(ttsReady && !saving);
        speak.setAlpha(speak.isEnabled() ? 1f : 0.4f);
        findViewById(R.id.buttonSpeakBack).setEnabled(ttsReady && !saving);
        findViewById(R.id.buttonSpeakBack).setAlpha(ttsReady && !saving ? 1f : 0.4f);
        findViewById(R.id.buttonSpeakExample).setVisibility(hasText(current.getExample()) ? View.VISIBLE : View.GONE);
        findViewById(R.id.buttonSpeakExample).setEnabled(ttsReady && !saving && hasText(current.getExample()));
        findViewById(R.id.buttonSpeakExample).setAlpha(ttsReady && !saving ? 1f : 0.4f);
        setOnlineActionAvailability(!duePending);
        maybeAutoSpeak(current);
    }

    private void loadCardProgress(int cardId) {
        final int version = cardVersion;
        final int expectedFavoriteVersion = favoriteVersion;
        progressRepository.getProgress(cardId, new ProgressRepository.ProgressCallback() {
            @Override
            public void onSuccess(ProgressDto progress) {
                if (!isCurrentOnlineCard(cardId, version) || favoriteVersion != expectedFavoriteVersion) return;
                isCurrentFavorite = progress.isFavorite();
                if (buttonFavorite != null) {
                    buttonFavorite.setText(isCurrentFavorite ? "❤️" : "🤍");
                }
            }

            @Override
            public void onError(String message) {
                // Silently keep default
            }
        });
    }

    private void setOnlineActionAvailability(boolean available) {
        available = available && !reviewPending;
        if (buttonFavorite != null) {
            buttonFavorite.setEnabled(available && !favoritePending);
            buttonFavorite.setAlpha(available ? 1f : 0.4f);
        }
        if (buttonNotRemember != null) buttonNotRemember.setEnabled(available && !reviewPending
                && !Boolean.TRUE.equals(viewModel.getReviewPending().getValue()));
        if (buttonRemembered != null) buttonRemembered.setEnabled(available && !reviewPending
                && !Boolean.TRUE.equals(viewModel.getReviewPending().getValue()));
        if (buttonNotRemember != null) buttonNotRemember.setAlpha(buttonNotRemember.isEnabled() ? 1f : 0.4f);
        if (buttonRemembered != null) buttonRemembered.setAlpha(buttonRemembered.isEnabled() ? 1f : 0.4f);
        if (buttonStartQuiz != null) {
            buttonStartQuiz.setEnabled(available);
            buttonStartQuiz.setAlpha(available ? 1f : 0.4f);
        }
    }

    private boolean canUseOnlineActions() {
        return !Boolean.TRUE.equals(viewModel.getLoading().getValue());
    }

    private boolean isCurrentOnlineCard(int cardId, int version) {
        return !destroyed && cardVersion == version && displayedCardId == cardId
                && !Boolean.TRUE.equals(viewModel.getLoading().getValue());
    }

    private static boolean hasText(@Nullable String text) {
        return text != null && !text.trim().isEmpty();
    }

    private static void optionalText(TextView view, @Nullable String value) {
        view.setText(value);
        view.setVisibility(hasText(value) ? View.VISIBLE : View.GONE);
    }

    private void initializeSpeech() {
        speechStatus.setText(R.string.flashcard_speech_initializing);
        try {
            // Post even an immediate init failure so the tts field has been assigned first.
            tts = new TextToSpeech(getApplicationContext(), status ->
                    mainHandler.post(() -> configureSpeech(status)));
        } catch (RuntimeException failure) {
            speechStatus.setText(R.string.flashcard_speech_unavailable);
        }
    }

    private void configureSpeech(int status) {
        if (destroyed || tts == null) return;
        if (status != TextToSpeech.SUCCESS) {
            speechStatus.setText(R.string.flashcard_speech_unavailable);
            return;
        }
        try {
            int language = tts.setLanguage(Locale.UK);
            if (language < TextToSpeech.LANG_AVAILABLE) language = tts.setLanguage(Locale.US);
            if (language < TextToSpeech.LANG_AVAILABLE) {
                speechStatus.setText(R.string.flashcard_speech_missing_english);
                return;
            }
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) { updateSpeechStatus(id, R.string.flashcard_speech_speaking); }
                @Override public void onDone(String id) { updateSpeechStatus(id, R.string.flashcard_speech_done); }
                @Override public void onError(String id) { updateSpeechStatus(id, R.string.flashcard_speech_failed); }
                @Override public void onError(String id, int code) { updateSpeechStatus(id, R.string.flashcard_speech_failed); }
            });
            ttsReady = true;
            tts.setSpeechRate(learningSettings.getSpeechRate());
            speechStatus.setText(R.string.flashcard_speech_ready);
            render();
        } catch (RuntimeException failure) {
            ttsReady = false;
            speechStatus.setText(R.string.flashcard_speech_unavailable);
        }
    }

    private void updateSpeechStatus(String id, int message) {
        mainHandler.post(() -> {
            if (!destroyed && id.equals(activeUtterance)) speechStatus.setText(message);
        });
    }

    private void speakCurrentWord() {
        List<Flashcard> cards = viewModel.getFlashcards().getValue();
        Integer index = viewModel.getCurrentIndex().getValue();
        if (!ttsReady || tts == null || cards == null || index == null || index < 0 || index >= cards.size()) return;
        Flashcard current = cards.get(index);
        speakText(current.getEnglish());
    }

    private void speakText(@Nullable String text) {
        if (!ttsReady || tts == null || !hasText(text) || destroyed) return;
        activeUtterance = "flashcard-" + displayedCardId + "-" + System.nanoTime();
        try {
            tts.setSpeechRate(learningSettings.getSpeechRate());
            int result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, activeUtterance);
            if (result == TextToSpeech.ERROR) speechStatus.setText(R.string.flashcard_speech_failed);
        } catch (RuntimeException failure) {
            speechStatus.setText(R.string.flashcard_speech_failed);
        }
    }

    private void maybeAutoSpeak(Flashcard current) {
        if (!ttsReady || tts == null || !learningSettings.isAutoPronunciationEnabled()
                || current.getId() == autoSpokenCardId || isFinishing() || destroyed) return;
        autoSpokenCardId = current.getId();
        speakCurrentWord();
    }

    private void applyLearningTextSize() {
        english.setTextSize(learningSettings.scaleText(36f));
        pronunciation.setTextSize(learningSettings.scaleText(16f));
        metadata.setTextSize(learningSettings.scaleText(12f));
        vietnamese.setTextSize(learningSettings.scaleText(24f));
        ((TextView)findViewById(R.id.textBackWord)).setTextSize(learningSettings.scaleText(30f));
        backPronunciation.setTextSize(learningSettings.scaleText(16f));
        backMetadata.setTextSize(learningSettings.scaleText(12f));
        example.setTextSize(learningSettings.scaleText(16f));
        exampleVi.setTextSize(learningSettings.scaleText(14f));
    }

    private void stopSpeech() {
        activeUtterance = null;
        if (tts != null && ttsReady) tts.stop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        sessionUi.putBundle(sessionKey,captureLearningUi());
        outState.putBundle(STATE_LEARNING_UI,sessionUi);
        outState.putString(STATE_LEARNING_KEY,sessionKey);
        outState.putString(STATE_BROWSE_FAILURE,browseReviewFailure);
        outState.putBoolean(STATE_BROWSE_ACK, browseReviewAcknowledged);
        outState.putInt(STATE_FLIPPED_CARD, isCardFlipped ? displayedCardId : -1);
        outState.putInt(STATE_AUTO_SPOKEN_CARD, autoSpokenCardId);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || session.getUserId() != sessionOwnerId) finish();
        else {
            applyLearningTextSize();
            if (ttsReady && tts != null) tts.setSpeechRate(learningSettings.getSpeechRate());
            render();
        }
    }

    @Override
    protected void onStop() {
        stopSpeech();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        View page = findViewById(R.id.flashcardPageScroll);
        page.removeCallbacks(resizeViewportTask);
        if (page.getViewTreeObserver().isAlive())
            page.getViewTreeObserver().removeOnGlobalLayoutListener(viewportLayoutListener);
        mainHandler.removeCallbacksAndMessages(null);
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        ttsReady = false;
        super.onDestroy();
    }
}
