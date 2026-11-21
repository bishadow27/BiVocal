package com.example.bivoca.ui.leaderboard;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bivoca.R;
import com.example.bivoca.data.model.LeaderboardUser;
import com.example.bivoca.ui.navigation.NavigationDrawerController;
import com.example.bivoca.ui.navigation.LearningBottomNavigation;
import com.example.bivoca.ui.navigation.NavigationRoute;
import com.example.bivoca.utils.SessionManager;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class BangXepHangActivity extends AppCompatActivity {

    private LeaderboardViewModel viewModel;
    private LeaderboardAdapter adapter;
    private int currentUserId;
    private final NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

    private ProgressBar progressLoading;
    private View errorContainer;
    private TextView textLeaderboardError;
    private Button buttonLeaderboardRetry;
    private View emptyContainer;
    private View layoutLeaderboardContent;
    private RecyclerView recyclerLeaderboard;

    // Tabs
    private TextView tabLeaderboardAll;
    private TextView tabLeaderboardMonth;
    private TextView tabLeaderboardTest;

    // Podium Views
    private View podiumCol1;
    private TextView textPodiumAvatar1;
    private TextView textPodiumName1;
    private TextView textPodiumScore1;

    private View podiumCol2;
    private TextView textPodiumAvatar2;
    private TextView textPodiumName2;
    private TextView textPodiumScore2;

    private View podiumCol3;
    private TextView textPodiumAvatar3;
    private TextView textPodiumName3;
    private TextView textPodiumScore3;

    // Sticky Bottom Bar
    private View layoutUserStickyBar;
    private TextView textStickyUserRank;
    private TextView textStickyUserScore;
    private View buttonHistoryPoints;
    private View buttonLeaderboardHelp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bang_xep_hang);

        currentUserId = new SessionManager(this).getUserId();

        NavigationDrawerController.attach(
                this,
                NavigationRoute.LEADERBOARD,
                R.id.buttonLeaderboardBack
        );

        LearningBottomNavigation.bind(this, NavigationRoute.LEADERBOARD);

        initViews();
        configureSelfFooter();
        setupTabs();
        setupActionButtons();

        adapter = new LeaderboardAdapter(currentUserId);
        recyclerLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        recyclerLeaderboard.setAdapter(adapter);

        buttonLeaderboardRetry.setOnClickListener(v -> viewModel.loadLeaderboard());

        viewModel = new ViewModelProvider(this).get(LeaderboardViewModel.class);
        viewModel.getLoading().observe(this, loading -> render());
        viewModel.getError().observe(this, error -> render());
        viewModel.getLeaderboard().observe(this, users -> render());

        if (viewModel.getLeaderboard().getValue() == null
                && !Boolean.TRUE.equals(viewModel.getLoading().getValue())
                && viewModel.getError().getValue() == null) {
            viewModel.loadLeaderboard();
        }
    }

    private void initViews() {
        progressLoading = findViewById(R.id.progressLoading);
        errorContainer = findViewById(R.id.errorContainer);
        textLeaderboardError = findViewById(R.id.textLeaderboardError);
        buttonLeaderboardRetry = findViewById(R.id.buttonLeaderboardRetry);
        emptyContainer = findViewById(R.id.emptyContainer);
        layoutLeaderboardContent = findViewById(R.id.layoutLeaderboardContent);
        recyclerLeaderboard = findViewById(R.id.recyclerLeaderboard);

        tabLeaderboardAll = findViewById(R.id.tabLeaderboardAll);
        tabLeaderboardMonth = findViewById(R.id.tabLeaderboardMonth);
        tabLeaderboardTest = findViewById(R.id.tabLeaderboardTest);

        podiumCol1 = findViewById(R.id.podiumCol1);
        textPodiumAvatar1 = findViewById(R.id.textPodiumAvatar1);
        textPodiumName1 = findViewById(R.id.textPodiumName1);
        textPodiumScore1 = findViewById(R.id.textPodiumScore1);

        podiumCol2 = findViewById(R.id.podiumCol2);
        textPodiumAvatar2 = findViewById(R.id.textPodiumAvatar2);
        textPodiumName2 = findViewById(R.id.textPodiumName2);
        textPodiumScore2 = findViewById(R.id.textPodiumScore2);

        podiumCol3 = findViewById(R.id.podiumCol3);
        textPodiumAvatar3 = findViewById(R.id.textPodiumAvatar3);
        textPodiumName3 = findViewById(R.id.textPodiumName3);
        textPodiumScore3 = findViewById(R.id.textPodiumScore3);

        layoutUserStickyBar = findViewById(R.id.layoutUserStickyBar);
        textStickyUserRank = findViewById(R.id.textStickyUserRank);
        textStickyUserScore = findViewById(R.id.textStickyUserScore);
        buttonHistoryPoints = findViewById(R.id.buttonHistoryPoints);
        buttonLeaderboardHelp = findViewById(R.id.buttonLeaderboardHelp);
    }

    /** Presentation only: keep full action labels readable with narrow width or large fonts. */
    private void configureSelfFooter() {
        int widthDp = getResources().getConfiguration().screenWidthDp;
        boolean stacked = widthDp < 380
                || (getResources().getConfiguration().fontScale > 1.15f && widthDp < 600);
        boolean wide = widthDp >= 600;
        LinearLayout card = findViewById(R.id.layoutLeaderboardSelfCard);
        LinearLayout overview = findViewById(R.id.layoutLeaderboardSelfOverview);
        LinearLayout actions = findViewById(R.id.layoutLeaderboardSelfActions);
        card.setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        overview.setOrientation(stacked || wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        overview.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams info = (LinearLayout.LayoutParams) overview.getLayoutParams();
        info.width = stacked ? LinearLayout.LayoutParams.MATCH_PARENT : 0;
        info.weight = stacked ? 0 : 1;
        info.bottomMargin = stacked ? Math.round(8 * getResources().getDisplayMetrics().density) : 0;
        overview.setLayoutParams(info);
        LinearLayout.LayoutParams rank = (LinearLayout.LayoutParams) textStickyUserRank.getLayoutParams();
        rank.width = stacked ? 0 : LinearLayout.LayoutParams.WRAP_CONTENT;
        rank.weight = stacked ? 1 : 0;
        textStickyUserRank.setLayoutParams(rank);
        LinearLayout.LayoutParams actionParams = (LinearLayout.LayoutParams) actions.getLayoutParams();
        actionParams.width = stacked ? LinearLayout.LayoutParams.MATCH_PARENT : LinearLayout.LayoutParams.WRAP_CONTENT;
        actions.setLayoutParams(actionParams);
        LinearLayout.LayoutParams history = (LinearLayout.LayoutParams) buttonHistoryPoints.getLayoutParams();
        history.width = stacked ? 0 : LinearLayout.LayoutParams.WRAP_CONTENT;
        history.weight = stacked ? 1 : 0;
        buttonHistoryPoints.setLayoutParams(history);
        if (wide) {
            float density = getResources().getDisplayMetrics().density;
            card.setPaddingRelative(card.getPaddingStart(), Math.round(6 * density),
                    card.getPaddingEnd(), Math.round(6 * density));
            layoutUserStickyBar.setPaddingRelative(layoutUserStickyBar.getPaddingStart(), Math.round(4 * density),
                    layoutUserStickyBar.getPaddingEnd(), Math.round(4 * density));
            View scoreGroup = (View) textStickyUserScore.getParent();
            LinearLayout.LayoutParams score = (LinearLayout.LayoutParams) scoreGroup.getLayoutParams();
            score.setMarginStart(Math.round(12 * density));
            score.topMargin = 0;
            scoreGroup.setLayoutParams(score);
            View scrollContent = ((android.view.ViewGroup) findViewById(R.id.leaderboardScroll)).getChildAt(0);
            scrollContent.setPaddingRelative(scrollContent.getPaddingStart(), scrollContent.getPaddingTop(),
                    scrollContent.getPaddingEnd(), Math.round(8 * density));
        }
    }

    private void setupTabs() {
        if (tabLeaderboardAll == null) return;

        tabLeaderboardAll.setOnClickListener(v -> selectTab(tabLeaderboardAll, "all"));
        tabLeaderboardMonth.setOnClickListener(v -> selectTab(tabLeaderboardMonth, "month"));
        tabLeaderboardTest.setOnClickListener(v -> selectTab(tabLeaderboardTest, "test"));
    }

    private void selectTab(TextView selectedTab, String tabKey) {
        TextView[] allTabs = {tabLeaderboardAll, tabLeaderboardMonth, tabLeaderboardTest};
        for (TextView t : allTabs) {
            if (t == selectedTab) {
                t.setBackgroundResource(R.drawable.lb_light_segment_selected);
                t.setTextColor(getColor(R.color.lb_light_cobalt));
            } else {
                t.setBackground(null);
                t.setTextColor(getColor(R.color.lb_light_secondary));
            }
        }
        Toast.makeText(this, "Bảng: " + selectedTab.getText(), Toast.LENGTH_SHORT).show();
    }

    private void setupActionButtons() {
        if (buttonHistoryPoints != null) {
            buttonHistoryPoints.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Lịch sử điểm tích lũy")
                        .setMessage("Bạn nhận được XP từ:\n• Hoàn thành bài học từ mới (+10 XP)\n• Ôn tập phân cách quãng SRS (+5 XP)\n• Trả lời chính xác câu hỏi Quiz (+15 XP)")
                        .setPositiveButton("Đã hiểu", null)
                        .show();
            });
        }

        if (buttonLeaderboardHelp != null) {
            buttonLeaderboardHelp.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Quy tắc xếp hạng")
                        .setMessage("Bảng xếp hạng cập nhật dựa trên tổng điểm XP bạn đạt được. Top 3 học viên dẫn đầu sẽ được vinh danh trên bục danh dự!")
                        .setPositiveButton("Đóng", null)
                        .show();
            });
        }
    }

    private void render() {
        boolean isLoading = Boolean.TRUE.equals(viewModel.getLoading().getValue());
        String errorMessage = viewModel.getError().getValue();
        List<LeaderboardUser> users = viewModel.getLeaderboard().getValue();

        progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);

        if (isLoading) {
            errorContainer.setVisibility(View.GONE);
            emptyContainer.setVisibility(View.GONE);
            layoutLeaderboardContent.setVisibility(View.GONE);
            return;
        }

        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            errorContainer.setVisibility(View.VISIBLE);
            textLeaderboardError.setText(errorMessage);
            emptyContainer.setVisibility(View.GONE);
            layoutLeaderboardContent.setVisibility(View.GONE);
            return;
        }

        errorContainer.setVisibility(View.GONE);

        if (users == null || users.isEmpty()) {
            emptyContainer.setVisibility(View.VISIBLE);
            layoutLeaderboardContent.setVisibility(View.GONE);
        } else {
            emptyContainer.setVisibility(View.GONE);
            layoutLeaderboardContent.setVisibility(View.VISIBLE);
            bindLeaderboardData(users);
        }
    }

    private void bindLeaderboardData(List<LeaderboardUser> users) {
        // Bind Podium Top 3
        if (users.size() >= 1) {
            LeaderboardUser u1 = users.get(0);
            podiumCol1.setVisibility(View.VISIBLE);
            textPodiumAvatar1.setText(getInitials(u1.getDisplayName()));
            textPodiumName1.setText(u1.getDisplayName());
            textPodiumScore1.setText(numberFormat.format(u1.getTotalXp()));
        } else {
            podiumCol1.setVisibility(View.INVISIBLE);
        }

        if (users.size() >= 2) {
            LeaderboardUser u2 = users.get(1);
            podiumCol2.setVisibility(View.VISIBLE);
            textPodiumAvatar2.setText(getInitials(u2.getDisplayName()));
            textPodiumName2.setText(u2.getDisplayName());
            textPodiumScore2.setText(numberFormat.format(u2.getTotalXp()));
        } else {
            podiumCol2.setVisibility(View.INVISIBLE);
        }

        if (users.size() >= 3) {
            LeaderboardUser u3 = users.get(2);
            podiumCol3.setVisibility(View.VISIBLE);
            textPodiumAvatar3.setText(getInitials(u3.getDisplayName()));
            textPodiumName3.setText(u3.getDisplayName());
            textPodiumScore3.setText(numberFormat.format(u3.getTotalXp()));
        } else {
            podiumCol3.setVisibility(View.INVISIBLE);
        }

        // Bind List for Rank 4+
        if (users.size() > 3) {
            adapter.setUsers(users.subList(3, users.size()));
        } else {
            adapter.setUsers(Collections.emptyList());
        }

        // Bind Sticky Bottom Bar for current user
        LeaderboardUser self = null;
        for (LeaderboardUser u : users) {
            if (u.getUserId() == currentUserId) {
                self = u;
                break;
            }
        }

        if (self != null && layoutUserStickyBar != null) {
            layoutUserStickyBar.setVisibility(View.VISIBLE);
            textStickyUserRank.setText("#" + self.getRank() + " · Bạn");
            textStickyUserScore.setText(numberFormat.format(self.getTotalXp()));
        } else if (layoutUserStickyBar != null && !users.isEmpty()) {
            layoutUserStickyBar.setVisibility(View.VISIBLE);
            LeaderboardUser first = users.get(0);
            textStickyUserRank.setText("#" + first.getRank() + " · Bạn");
            textStickyUserScore.setText(numberFormat.format(first.getTotalXp()));
        }
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "U";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        String first = parts[0].substring(0, 1);
        String last = parts[parts.length - 1].substring(0, 1);
        return (first + last).toUpperCase(Locale.ROOT);
    }
}
