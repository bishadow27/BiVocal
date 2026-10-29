package com.example.bivoca.ui.home;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.bivoca.R;
import com.example.bivoca.data.model.GamificationStats;
import com.example.bivoca.data.model.ReviewSchedule;
import com.example.bivoca.data.model.LearningToday;
import com.example.bivoca.data.model.LeaderboardUser;
import com.example.bivoca.ui.auth.LoginActivity;
import com.example.bivoca.ui.flashcard.FlashcardActivity;
import com.example.bivoca.ui.leaderboard.LeaderboardActivity;
import com.example.bivoca.ui.leaderboard.LeaderboardViewModel;
import com.example.bivoca.ui.navigation.NavigationDrawerController;
import com.example.bivoca.ui.navigation.NavigationRoute;
import com.example.bivoca.ui.profile.ProfileViewModel;
import com.example.bivoca.ui.progress.ProgressActivity;
import com.example.bivoca.ui.progress.ProgressViewModel;
import com.example.bivoca.ui.quiz.QuizActivity;
import com.example.bivoca.ui.topics.TopicsActivity;
import com.example.bivoca.utils.SessionManager;
import com.example.bivoca.settings.ReminderScheduler;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/** Native Home frontend over existing ViewModels and authenticated REST APIs. */
public class HomeActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private ProfileViewModel profileViewModel;
    private ProgressViewModel progressViewModel;
    private LeaderboardViewModel leaderboardViewModel;
    private TextView textDailyGoal, textDailyGoalStatus;
    private ProgressBar dailyGoalProgress, loading;
    private View errorContainer;
    private TextView textError;
    private boolean hasResumed;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) { openLogin(); return; }
        ReminderScheduler.reconcile(this);
        setContentView(R.layout.activity_home);
        androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightNavigationBars(false);
        NavigationDrawerController.attach(this, NavigationRoute.HOME, R.id.buttonHomeMenu);
        textDailyGoal = findViewById(R.id.textDailyGoal);
        textDailyGoalStatus = findViewById(R.id.textDailyGoalStatus);
        dailyGoalProgress = findViewById(R.id.progressDailyGoal);
        loading = findViewById(R.id.progressHomeLoading);
        errorContainer = findViewById(R.id.homeErrorContainer);
        textError = findViewById(R.id.textHomeError);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        progressViewModel = new ViewModelProvider(this).get(ProgressViewModel.class);
        leaderboardViewModel = new ViewModelProvider(this).get(LeaderboardViewModel.class);
        bindActions();
        bindMascot();
        observeData();
        if (profileViewModel.getUser().getValue() == null && profileViewModel.getStats().getValue() == null
                && !Boolean.TRUE.equals(profileViewModel.getLoading().getValue())
                && profileViewModel.getError().getValue() == null) profileViewModel.loadProfile();
        if (progressViewModel.getSummary().getValue() == null
                && !Boolean.TRUE.equals(progressViewModel.getLoading().getValue())
                && progressViewModel.getError().getValue() == null) progressViewModel.loadSummary();
        if (leaderboardViewModel.getLeaderboard().getValue() == null
                && !Boolean.TRUE.equals(leaderboardViewModel.getLoading().getValue())
                && leaderboardViewModel.getError().getValue() == null) leaderboardViewModel.loadLeaderboard();
        render();
    }

    private void bindActions() {
        com.example.bivoca.ui.navigation.LearningBottomNavigation.bind(this, NavigationRoute.HOME);
        findViewById(R.id.buttonProgress).setOnClickListener(v -> open(ProgressActivity.class));
        findViewById(R.id.buttonHomeSrsInfo).setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.home_srs_title).setMessage(R.string.home_srs_help)
                .setPositiveButton(R.string.home_srs_help_close, null).show());
        findViewById(R.id.buttonHomeSrsReview).setOnClickListener(v -> findViewById(R.id.buttonReviewToday).performClick());
        findViewById(R.id.buttonLeaderboard).setOnClickListener(v -> open(LeaderboardActivity.class));
        findViewById(R.id.buttonLearnNewToday).setOnClickListener(v -> {
            LearningToday today = progressViewModel.getLearningToday().getValue();
            if (today == null || progressViewModel.getLearningError().getValue() != null) return;
            Intent intent = new Intent(this, FlashcardActivity.class);
            intent.putExtra(FlashcardActivity.EXTRA_NEW_TODAY, true);
            intent.putExtra(FlashcardActivity.EXTRA_NEW_LIMIT, today.isCompleted() ? today.getDailyGoal() : Math.max(1, today.getRemaining()));
            startActivity(intent);
        });
        findViewById(R.id.buttonHomeRetry).setOnClickListener(v -> retry());
    }

    private void bindMascot() {
        Bitmap source = BitmapFactory.decodeResource(getResources(), R.drawable.bivocal_wordmark);
        if (source == null) return;
        int width = Math.min(source.getWidth(), Math.round(source.getWidth() * 128f / 414f));
        Bitmap mascot = Bitmap.createBitmap(source, 0, 0, width, source.getHeight());
        ((ImageView) findViewById(R.id.homeHeaderMascot)).setImageBitmap(mascot);
    }

    private void observeData() {
        profileViewModel.getLoading().observe(this, v -> render());
        profileViewModel.getUser().observe(this, v -> render());
        profileViewModel.getStats().observe(this, v -> render());
        profileViewModel.getError().observe(this, v -> render());
        progressViewModel.getLoading().observe(this, v -> render());
        progressViewModel.getSummary().observe(this, v -> render());
        progressViewModel.getSchedule().observe(this, v -> render());
        progressViewModel.getError().observe(this, v -> render());
        progressViewModel.getScheduleError().observe(this, v -> render());
        progressViewModel.getLearningToday().observe(this, v -> render());
        progressViewModel.getLearningError().observe(this, v -> render());
        leaderboardViewModel.getLoading().observe(this, v -> renderLeaderboard());
        leaderboardViewModel.getLeaderboard().observe(this, v -> renderLeaderboard());
        leaderboardViewModel.getError().observe(this, v -> renderLeaderboard());
    }

    private void render() {
        GamificationStats stats = profileViewModel.getStats().getValue();
        text(R.id.textHomeStreakBadge, getString(R.string.home16_streak_badge, stats == null ? "—" : number(stats.getCurrentStreak())));
        LearningToday today = progressViewModel.getLearningToday().getValue();
        boolean goalAvailable = today != null && progressViewModel.getLearningError().getValue() == null;
        findViewById(R.id.buttonLearnNewToday).setEnabled(goalAvailable);
        if (today == null) {
            textDailyGoal.setText("— / —");
            dailyGoalProgress.setProgress(0);
            text(R.id.textDailyGoalPercent, "—");
            textDailyGoalStatus.setText(progressViewModel.getLearningError().getValue() == null
                    ? R.string.home16_loading : R.string.home16_goal_unavailable);
        } else {
            android.text.SpannableString goal = new android.text.SpannableString(getString(R.string.home16_goal_value, today.getNewWordsLearned(), today.getDailyGoal()));
            int separator = goal.toString().indexOf('/');
            if (separator >= 0) {
                goal.setSpan(new android.text.style.RelativeSizeSpan(16f / 28f), separator, goal.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                goal.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#94A3B8")), separator, goal.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            textDailyGoal.setText(goal);
            dailyGoalProgress.setProgress(Math.max(0, Math.min(100, today.getCompletionPercent())));
            text(R.id.textDailyGoalPercent, getString(R.string.home16_goal_percent, today.getCompletionPercent()));
            textDailyGoalStatus.setText(today.isCompleted() ? getString(R.string.home16_goal_done)
                    : getString(R.string.home16_goal_remaining, today.getRemaining()));
            if (!today.isCompleted()) {
                android.text.SpannableString status = new android.text.SpannableString(textDailyGoalStatus.getText());
                int start = status.toString().indexOf(String.valueOf(today.getRemaining()));
                int end = status.toString().indexOf(" từ nữa", start);
                if (start >= 0 && end >= start) {
                    end += " từ nữa".length();
                    status.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#38BDF8")), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    status.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                textDailyGoalStatus.setText(status);
            }
        }
        ((TextView) findViewById(R.id.buttonLearnNewToday)).setText(today != null && today.isCompleted()
                ? R.string.home16_learn_more : R.string.home16_learn);
        renderSrs();
        loading.setVisibility(Boolean.TRUE.equals(profileViewModel.getLoading().getValue())
                || Boolean.TRUE.equals(progressViewModel.getLoading().getValue()) ? View.VISIBLE : View.GONE);
        List<String> errors = new ArrayList<>();
        addError(errors, profileViewModel.getError().getValue());
        addError(errors, progressViewModel.getError().getValue());
        addError(errors, progressViewModel.getScheduleError().getValue());
        addError(errors, progressViewModel.getLearningError().getValue());
        errorContainer.setVisibility(errors.isEmpty() ? View.GONE : View.VISIBLE);
        textError.setText(String.join(" ", errors));
        renderLeaderboard();
    }

    private void renderSrs() {
        ReviewSchedule schedule = progressViewModel.getSchedule().getValue();
        int[] groups = schedule == null ? null : HomePresentation.srsGroups(schedule.getMemoryLevels());
        if (groups != null && groups[0] + groups[1] + groups[2] + groups[3] != schedule.getReviewedCards()) groups = null;
        int[] labels = {R.id.textHomeNew, R.id.textHomeRemembering, R.id.textHomeGood, R.id.textHomeMastered};
        int[] segments = {R.id.homeSrsNewSegment, R.id.homeSrsRememberingSegment, R.id.homeSrsGoodSegment, R.id.homeSrsMasteredSegment};
        for (int i = 0; i < 4; i++) {
            text(labels[i], groups == null ? "—" : number(groups[i]));
            View segment = findViewById(segments[i]);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) segment.getLayoutParams();
            params.weight = groups == null ? 0 : groups[i];
            segment.setLayoutParams(params);
            segment.setVisibility(groups != null && groups[i] > 0 ? View.VISIBLE : View.GONE);
        }
        text(R.id.textHomeSrsTotal, groups == null ? "—" : number(schedule.getReviewedCards()));
        int total = groups == null ? 0 : schedule.getReviewedCards();
        int masteredPercent = total == 0 ? 0 : Math.round(groups[3] * 100f / total);
        text(R.id.textHomeSrsCaption, groups == null ? getString(R.string.home16_no_data)
                : getString(R.string.home_srs_caption, masteredPercent));
        boolean canReview = groups != null && schedule.getDueNow() > 0;
        View review = findViewById(R.id.buttonHomeSrsReview);
        review.setEnabled(canReview);
        review.setAlpha(canReview ? 1f : 0.65f);
        findViewById(R.id.textHomeSrsDue).setVisibility(canReview ? View.VISIBLE : View.GONE);
        text(R.id.textHomeSrsDueLabel, groups == null ? getString(R.string.home16_no_data)
                : canReview ? getString(R.string.home_srs_due, number(schedule.getDueNow())) : getString(R.string.home_srs_no_due));
    }

    private void renderLeaderboard() {
        if (leaderboardViewModel == null) return;
        LinearLayout rows = findViewById(R.id.homeLeaderboardRows);
        rows.removeAllViews();
        List<LeaderboardUser> users = leaderboardViewModel.getLeaderboard().getValue();
        String error = leaderboardViewModel.getError().getValue();
        TextView state = findViewById(R.id.textHomeLeaderboardState);
        if (error != null) {
            state.setVisibility(View.VISIBLE);
            state.setText(error);
            state.setOnClickListener(v -> leaderboardViewModel.loadLeaderboard());
            return;
        }
        state.setOnClickListener(null);
        if (users == null || users.isEmpty()) {
            state.setVisibility(View.VISIBLE);
            state.setText(Boolean.TRUE.equals(leaderboardViewModel.getLoading().getValue())
                    ? R.string.home16_loading : R.string.home16_leaderboard_empty);
            return;
        }
        boolean containsMe = false;
        for (LeaderboardUser user : HomePresentation.preview(users, sessionManager.getUserId())) {
            View row = getLayoutInflater().inflate(R.layout.item_home_leaderboard, rows, false);
            boolean me = user.getUserId() == sessionManager.getUserId();
            containsMe |= me;
            if (me) row.setBackgroundResource(R.drawable.home16_row_self);
            else if (user.getRank() == 1) row.setBackgroundResource(R.drawable.home16_rank_gold);
            else if (user.getRank() == 2) row.setBackgroundResource(R.drawable.home16_rank_silver);
            TextView rank = row.findViewById(R.id.homeRank);
            rank.setText(me || user.getRank() > 3 ? "#" + user.getRank() : user.getRank() == 1 ? "🥇" : user.getRank() == 2 ? "🥈" : "🥉");
            rank.setContentDescription("Hạng " + user.getRank());
            if (me) ((TextView) row.findViewById(R.id.homeRankName)).setTextColor(android.graphics.Color.parseColor("#38BDF8"));
            if (!me && user.getRank() == 1) ((TextView) row.findViewById(R.id.homeRankXp)).setTextColor(android.graphics.Color.parseColor("#FBBF24"));
            ((TextView) row.findViewById(R.id.homeRankAvatar)).setText(com.example.bivoca.ui.navigation.AvatarLabel.initial(user.getDisplayName()));
            ((TextView) row.findViewById(R.id.homeRankName)).setText(me ? getString(R.string.home16_leaderboard_self, user.getDisplayName()) : user.getDisplayName());
            ((TextView) row.findViewById(R.id.homeRankMeta)).setText(getString(R.string.home16_leaderboard_meta, user.getCurrentStreak(), user.getLevel()));
            ((TextView) row.findViewById(R.id.homeRankXp)).setText(getString(R.string.home16_xp_value, number(user.getTotalXp())));
            rows.addView(row);
        }
        state.setVisibility(containsMe ? View.GONE : View.VISIBLE);
        if (!containsMe) state.setText(getString(R.string.home16_leaderboard_user_missing, users.size()));
    }

    @Override protected void onResume() {
        super.onResume();
        if (hasResumed && profileViewModel != null) retry();
        hasResumed = true;
    }
    private void retry() { profileViewModel.loadProfile(); progressViewModel.loadSummary(); leaderboardViewModel.loadLeaderboard(); }
    private void text(int id, String value) { ((TextView) findViewById(id)).setText(value); }
    private static String number(int value) { return NumberFormat.getIntegerInstance().format(value); }
    private void open(Class<?> destination) {
        Intent intent = new Intent(this, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent); finish();
    }
    private static void addError(List<String> errors, String error) {
        if (error != null && !error.trim().isEmpty() && !errors.contains(error)) errors.add(error);
    }
}

