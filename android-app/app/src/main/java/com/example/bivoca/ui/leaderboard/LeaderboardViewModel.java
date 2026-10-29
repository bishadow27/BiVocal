package com.example.bivoca.ui.leaderboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bivoca.data.model.LeaderboardUser;
import com.example.bivoca.data.repository.LeaderboardRepository;

import java.util.List;

import retrofit2.Call;

public class LeaderboardViewModel extends AndroidViewModel {

    private final LeaderboardRepository repository;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<List<LeaderboardUser>> leaderboard = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    @Nullable private Call<List<LeaderboardUser>> activeCall;
    private int requestVersion = 0;

    public LeaderboardViewModel(@NonNull Application application) {
        super(application);
        this.repository = new LeaderboardRepository(application.getApplicationContext());
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<List<LeaderboardUser>> getLeaderboard() {
        return leaderboard;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadLeaderboard() {
        loadLeaderboard(50);
    }

    public void loadLeaderboard(int limit) {
        cancelCall();
        int version = ++requestVersion;

        loading.setValue(true);
        error.setValue(null);

        activeCall = repository.getLeaderboard(Math.max(1, Math.min(100, limit)), new LeaderboardRepository.LeaderboardCallback() {
            @Override
            public void onSuccess(List<LeaderboardUser> users) {
                if (version != requestVersion) return;
                loading.setValue(false);
                leaderboard.setValue(users);
            }

            @Override
            public void onError(String message) {
                if (version != requestVersion) return;
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }

    private void cancelCall() {
        if (activeCall != null) {
            activeCall.cancel();
            activeCall = null;
        }
    }

    @Override
    protected void onCleared() {
        requestVersion++;
        cancelCall();
    }
}
