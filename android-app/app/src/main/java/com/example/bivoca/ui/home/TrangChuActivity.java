package com.example.bivoca.ui.home;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.bivoca.R;
import com.example.bivoca.data.model.GamificationStats;
import com.example.bivoca.data.model.User;
import com.example.bivoca.data.model.ReviewSchedule;
import com.example.bivoca.data.model.LearningToday;
import com.example.bivoca.data.model.LeaderboardUser;
import com.example.bivoca.ui.auth.DangNhapActivity;
import com.example.bivoca.ui.flashcard.TheTuVungActivity;
import com.example.bivoca.ui.leaderboard.BangXepHangActivity;
import com.example.bivoca.ui.leaderboard.LeaderboardViewModel;
import com.example.bivoca.ui.navigation.NavigationDrawerController;
import com.example.bivoca.ui.navigation.NavigationRoute;
import com.example.bivoca.ui.profile.ProfileViewModel;
import com.example.bivoca.ui.progress.TienDoHocTapActivity;
import com.example.bivoca.ui.progress.ProgressViewModel;
import com.example.bivoca.ui.quiz.TracNghiemActivity;
import com.example.bivoca.ui.topics.ChuDeActivity;
import com.example.bivoca.utils.SessionManager;
import com.example.bivoca.settings.ReminderScheduler;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/** Native Home frontend over existing ViewModels and authenticated REST APIs. */
public class TrangChuActivity extends AppCompatActivity {
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
        setContentView(R.layout.activity_trang_chu);
        androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
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
        findViewById(R.id.homeBottomNavigation).setBackgroundResource(R.color.home_brand_orange);
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
                && leaderboardViewModel.getError().getValue() == null) leaderboardViewModel.loadLeaderboard(100);
        render();
    }

    private void bindActions() {
        com.example.bivoca.ui.navigation.LearningBottomNavigation.bind(this, NavigationRoute.HOME,
                getColor(R.color.home_brand_selected), getColor(R.color.brand_on_teal));
        ((android.view.ViewGroup)findViewById(R.id.homeTabHome)).getChildAt(0).setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.brand_logo_accent)));
        findViewById(R.id.textHomeAvatar).setBackgroundResource(R.drawable.home_approved_avatar);
        findViewById(R.id.buttonProgress).setOnClickListener(v -> open(TienDoHocTapActivity.class));
        findViewById(R.id.buttonHomeSrsInfo).setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.home_srs_title).setMessage(R.string.home_srs_help)
                .setPositiveButton(R.string.home_srs_help_close, null).show());
        findViewById(R.id.buttonHomeSrsReview).setOnClickListener(v -> findViewById(R.id.buttonReviewToday).performClick());
        findViewById(R.id.buttonLeaderboard).setOnClickListener(v -> open(BangXepHangActivity.class));
        findViewById(R.id.buttonLearnNewToday).setOnClickListener(v -> {
            LearningToday today = progressViewModel.getLearningToday().getValue();
            if (today == null || progressViewModel.getLearningError().getValue() != null) return;
            Intent intent = new Intent(this, TheTuVungActivity.class);
            intent.putExtra(TheTuVungActivity.EXTRA_NEW_TODAY, true);
            intent.putExtra(TheTuVungActivity.EXTRA_NEW_LIMIT, today.isCompleted() ? today.getDailyGoal() : Math.max(1, today.getRemaining()));
            com.example.bivoca.ui.navigation.LearningBottomNavigation.open(this, intent);
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
        text(R.id.textHomeStreakBadge, getString(R.string.home_approved_streak, stats == null ? "—" : number(stats.getCurrentStreak())));
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
                goal.setSpan(new android.text.style.RelativeSizeSpan(16f / 32f), separator, goal.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                goal.setSpan(new android.text.style.ForegroundColorSpan(getColor(R.color.home_approved_secondary_text)), separator, goal.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            textDailyGoal.setText(goal);
            dailyGoalProgress.setProgress(Math.max(0, Math.min(100, today.getCompletionPercent())));
            text(R.id.textDailyGoalPercent, getString(R.string.home_approved_goal_percent, today.getCompletionPercent()));
            textDailyGoalStatus.setText(today.isCompleted() ? getString(R.string.home16_goal_done)
                    : getString(R.string.home_approved_goal_remaining, today.getRemaining()));

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
        text(R.id.textHomeSrsCaption, groups == null ? getString(R.string.home16_no_data)
                : getString(R.string.home_approved_learned));
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
        LinearLayout ownRows = findViewById(R.id.homeCurrentUserRows);
        rows.removeAllViews();
        ownRows.removeAllViews();
        List<LeaderboardUser> users = leaderboardViewModel.getLeaderboard().getValue();
        String error = leaderboardViewModel.getError().getValue();
        TextView state = findViewById(R.id.textHomeLeaderboardState);
        state.setOnClickListener(null);
        state.setVisibility(View.GONE);
        if (error != null) {
            state.setVisibility(View.VISIBLE);
            state.setText(error);
            state.setOnClickListener(v -> leaderboardViewModel.loadLeaderboard(100));
        } else if (users == null || users.isEmpty()) {
            state.setVisibility(View.VISIBLE);
            state.setText(Boolean.TRUE.equals(leaderboardViewModel.getLoading().getValue())
                    ? R.string.home16_loading : R.string.home16_leaderboard_empty);
        }
        List<LeaderboardUser> top = HomePresentation.topThree(error == null ? users : null);
        for (LeaderboardUser user : top) {
            addRankRow(rows, String.valueOf(user.getRank()), user.getDisplayName(), user.getTotalXp(),
                    user.getUserId() == sessionManager.getUserId(), user.getRank() == 1, null);
            View divider = new View(this);
            divider.setBackgroundColor(getColor(R.color.home_approved_border));
            rows.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        }
        // Membership is by user ID, not rank: ties can occupy several of the first three rows.
        if (HomePresentation.findUser(top, sessionManager.getUserId()) != null) return;
        LeaderboardUser me = error == null ? HomePresentation.findUser(users, sessionManager.getUserId()) : null;
        if (me != null) {
            addRankRow(ownRows, String.valueOf(me.getRank()), me.getDisplayName(), me.getTotalXp(), true, false, null);
            return;
        }
        User profile = profileViewModel.getUser().getValue();
        GamificationStats stats = profileViewModel.getStats().getValue();
        if (profile == null || stats == null || profile.getId() != sessionManager.getUserId()) return;
        String name = profile.getFullName();
        if (name == null || name.trim().isEmpty()) name = profile.getUsername();
        // The bounded API cannot supply every user's rank. Never infer it from XP or list length.
        addRankRow(ownRows, "—", name, stats.getTotalXp(), true, false,
                getString(stats.getTotalXp() == 0 ? R.string.home_approved_not_ranked : R.string.home_approved_rank_unavailable));
    }

    private void addRankRow(LinearLayout parent, String rank, String name, int xp, boolean me, boolean first, String note) {
        View row = getLayoutInflater().inflate(R.layout.item_home_leaderboard, parent, false);
        TextView rankView = row.findViewById(R.id.homeRank);
        TextView nameView = row.findViewById(R.id.homeRankName);
        TextView xpView = row.findViewById(R.id.homeRankXp);
        rankView.setText(rank);
        nameView.setText(me ? getString(R.string.home_approved_self, name) : name);
        xpView.setText(getString(R.string.home16_xp_value, number(xp)));
        ((TextView) row.findViewById(R.id.homeRankAvatar)).setText(com.example.bivoca.ui.navigation.AvatarLabel.initial(name));
        if (me) {
            row.setBackgroundResource(R.drawable.home_approved_self);
            nameView.setTextColor(getColor(R.color.home_approved_cyan));
            xpView.setTextColor(getColor(R.color.home_approved_cyan));
        }
        if (first || "2".equals(rank) || "3".equals(rank)) {
            int background = first ? R.drawable.quiz_v2_method_1
                    : "2".equals(rank) ? R.drawable.quiz_v2_method_2 : R.drawable.quiz_v2_method_5;
            row.setBackgroundResource(background);
            row.setPadding(dp(12), row.getPaddingTop(), dp(12), row.getPaddingBottom());
            LinearLayout.LayoutParams spacing = (LinearLayout.LayoutParams) row.getLayoutParams();
            spacing.topMargin = dp(4);
            spacing.bottomMargin = dp(4);
            rankView.setTextColor(android.graphics.Color.WHITE);
            nameView.setTextColor(android.graphics.Color.WHITE);
            xpView.setTextColor(android.graphics.Color.WHITE);
            TextView avatar = row.findViewById(R.id.homeRankAvatar);
            android.graphics.drawable.GradientDrawable avatarBackground = new android.graphics.drawable.GradientDrawable();
            avatarBackground.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            avatarBackground.setColor(0x33FFFFFF);
            avatar.setBackground(avatarBackground);
            avatar.setTextColor(android.graphics.Color.WHITE);
        }
        if (note != null) {
            TextView meta = row.findViewById(R.id.homeRankMeta);
            meta.setText(note);
            meta.setVisibility(View.VISIBLE);
        }
        row.setContentDescription(getString(R.string.home_approved_rank_description, rank, name, number(xp)));
        parent.addView(row);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override protected void onResume() {
        super.onResume();
        if (hasResumed && profileViewModel != null) retry();
        hasResumed = true;
    }
    private void retry() { profileViewModel.loadProfile(); progressViewModel.loadSummary(); leaderboardViewModel.loadLeaderboard(100); }
    private void text(int id, String value) { ((TextView) findViewById(id)).setText(value); }
    private static String number(int value) { return NumberFormat.getIntegerInstance().format(value); }
    private void open(Class<?> destination) {
        Intent intent = new Intent(this, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        com.example.bivoca.ui.navigation.LearningBottomNavigation.open(this,intent);
    }
    private void openLogin() {
        Intent intent = new Intent(this, DangNhapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent); finish();
    }
    private static void addError(List<String> errors, String error) {
        if (error != null && !error.trim().isEmpty() && !errors.contains(error)) errors.add(error);
    }
}

