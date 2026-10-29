package com.example.bivoca.ui.progress;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.bivoca.R;
import com.example.bivoca.data.model.ProgressSummary;
import com.example.bivoca.data.model.ReviewSchedule;
import com.example.bivoca.data.model.LearningHistory;
import com.example.bivoca.data.model.LearningDay;
import com.example.bivoca.ui.navigation.NavigationDrawerController;
import com.example.bivoca.ui.navigation.NavigationRoute;

import java.util.Locale;

public class ProgressActivity extends AppCompatActivity {

    private ProgressViewModel viewModel;

    private ProgressBar progressLoading;
    private View errorContainer;
    private TextView textProgressError;
    private Button buttonProgressRetry;
    private ScrollView scrollProgressContent;

    private TextView textAccuracy;
    private TextView textTrackedCards;
    private TextView textReviewedCards;
    private TextView textMasteredCards;
    private TextView textFavoriteCards;
    private TextView textTotalReviews;
    private TextView textCorrectAnswers;
    private TextView textWrongAnswers;
    private TextView textDueNow;
    private TextView textUpcoming;
    private TextView textReviewedToday;
    private TextView textScheduleStatus;
    private WeeklyActivityChartView weeklyChart;
    private TextView textGoalHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        NavigationDrawerController.attach(
                this,
                NavigationRoute.PROGRESS,
                R.id.buttonProgressBack
        );

        progressLoading = findViewById(R.id.progressLoading);
        errorContainer = findViewById(R.id.errorContainer);
        textProgressError = findViewById(R.id.textProgressError);
        buttonProgressRetry = findViewById(R.id.buttonProgressRetry);
        scrollProgressContent = findViewById(R.id.scrollProgressContent);

        textAccuracy = findViewById(R.id.textAccuracy);
        textTrackedCards = findViewById(R.id.textTrackedCards);
        textReviewedCards = findViewById(R.id.textReviewedCards);
        textMasteredCards = findViewById(R.id.textMasteredCards);
        textFavoriteCards = findViewById(R.id.textFavoriteCards);
        textTotalReviews = findViewById(R.id.textTotalReviews);
        textCorrectAnswers = findViewById(R.id.textCorrectAnswers);
        textWrongAnswers = findViewById(R.id.textWrongAnswers);
        textDueNow = findViewById(R.id.textDueNow);
        textUpcoming = findViewById(R.id.textUpcoming);
        textReviewedToday = findViewById(R.id.textReviewedToday);
        textScheduleStatus = findViewById(R.id.textScheduleStatus);
        weeklyChart = findViewById(R.id.weeklyChart);
        textGoalHistory = findViewById(R.id.textGoalHistory);

        buttonProgressRetry.setOnClickListener(v -> viewModel.loadSummary());

        viewModel = new ViewModelProvider(this).get(ProgressViewModel.class);
        viewModel.getLoading().observe(this, loading -> render());
        viewModel.getError().observe(this, error -> render());
        viewModel.getSummary().observe(this, summary -> render());
        viewModel.getSchedule().observe(this, schedule -> render());
        viewModel.getScheduleError().observe(this, scheduleError -> render());
        viewModel.getLearningHistory().observe(this, history -> render());
        viewModel.getLearningToday().observe(this, today -> render());
        viewModel.getLearningError().observe(this, error -> render());

        if (viewModel.getSummary().getValue() == null
                && !Boolean.TRUE.equals(viewModel.getLoading().getValue())
                && viewModel.getError().getValue() == null) {
            viewModel.loadSummary();
        }
    }

    private void render() {
        boolean isLoading = Boolean.TRUE.equals(viewModel.getLoading().getValue());
        String errorMessage = viewModel.getError().getValue();
        ProgressSummary summary = viewModel.getSummary().getValue();

        progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);

        if (isLoading) {
            errorContainer.setVisibility(View.GONE);
            scrollProgressContent.setVisibility(View.GONE);
            return;
        }

        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            errorContainer.setVisibility(View.VISIBLE);
            textProgressError.setText(errorMessage);
            scrollProgressContent.setVisibility(View.GONE);
            return;
        }

        errorContainer.setVisibility(View.GONE);

        if (summary != null) {
            scrollProgressContent.setVisibility(View.VISIBLE);

            double accuracyVal = summary.getAccuracy() != null ? summary.getAccuracy() : 0.0;
            textAccuracy.setText(String.format(Locale.getDefault(), "%.0f%%", accuracyVal));

            textTrackedCards.setText(String.valueOf(summary.getTrackedCards()));
            textReviewedCards.setText(String.valueOf(summary.getReviewedCards()));
            textMasteredCards.setText(String.valueOf(summary.getMasteredCards()));
            textFavoriteCards.setText(String.valueOf(summary.getFavoriteCards()));

            textTotalReviews.setText(summary.getTotalReviews() + " lượt");
            textCorrectAnswers.setText(summary.getCorrectAnswers() + " lượt");
            textWrongAnswers.setText(summary.getWrongAnswers() + " lượt");
            LearningHistory history = viewModel.getLearningHistory().getValue();
            if (history != null) {
                weeklyChart.setDays(history.getDays()); int completed=0; StringBuilder rows=new StringBuilder();
                for (LearningDay day:history.getDays()) { if("COMPLETED".equals(day.getStatus()))completed++;
                    if(rows.length()>0)rows.append("  ·  "); rows.append(day.getDate().substring(5)).append(' ');
                    rows.append("COMPLETED".equals(day.getStatus())?"✓":"NO_DATA".equals(day.getStatus())?"—":"○"); }
                textGoalHistory.setText("Hoàn thành mục tiêu: "+completed+"/7 ngày\n"+rows);
            } else textGoalHistory.setText("Đang tải lịch sử mục tiêu…");
            ReviewSchedule schedule = viewModel.getSchedule().getValue();
            if (schedule != null) {
                textDueNow.setText(String.valueOf(schedule.getDueNow()));
                textUpcoming.setText(String.valueOf(schedule.getUpcoming24Hours()));
                textReviewedToday.setText(String.valueOf(schedule.getReviewedToday()));
                textScheduleStatus.setVisibility(View.GONE);
            } else {
                textDueNow.setText("—");
                textUpcoming.setText("—");
                textReviewedToday.setText("—");
                textScheduleStatus.setVisibility(View.VISIBLE);
                String scheduleError = viewModel.getScheduleError().getValue();
                textScheduleStatus.setText(scheduleError == null ? "Đang tải lịch ôn tập…" : scheduleError);
            }
        } else {
            scrollProgressContent.setVisibility(View.GONE);
        }
    }
}
