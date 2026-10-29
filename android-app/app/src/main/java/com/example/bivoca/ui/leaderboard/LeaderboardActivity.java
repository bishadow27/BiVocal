package com.example.bivoca.ui.leaderboard;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bivoca.R;
import com.example.bivoca.data.model.LeaderboardUser;
import com.example.bivoca.ui.navigation.NavigationDrawerController;
import com.example.bivoca.ui.navigation.NavigationRoute;

import java.util.List;
import com.example.bivoca.utils.SessionManager;

public class LeaderboardActivity extends AppCompatActivity {

    private LeaderboardViewModel viewModel;
    private LeaderboardAdapter adapter;

    private ProgressBar progressLoading;
    private View errorContainer;
    private TextView textLeaderboardError;
    private Button buttonLeaderboardRetry;
    private View emptyContainer;
    private View layoutLeaderboardContent;
    private RecyclerView recyclerLeaderboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        NavigationDrawerController.attach(
                this,
                NavigationRoute.LEADERBOARD,
                R.id.buttonLeaderboardBack
        );

        progressLoading = findViewById(R.id.progressLoading);
        errorContainer = findViewById(R.id.errorContainer);
        textLeaderboardError = findViewById(R.id.textLeaderboardError);
        buttonLeaderboardRetry = findViewById(R.id.buttonLeaderboardRetry);
        emptyContainer = findViewById(R.id.emptyContainer);
        layoutLeaderboardContent = findViewById(R.id.layoutLeaderboardContent);
        recyclerLeaderboard = findViewById(R.id.recyclerLeaderboard);

        adapter = new LeaderboardAdapter(new SessionManager(this).getUserId());
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
            adapter.setUsers(users);
        }
    }
}
