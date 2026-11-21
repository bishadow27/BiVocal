package com.example.bivoca.ui.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bivoca.data.model.GamificationStats;
import com.example.bivoca.data.model.User;
import com.example.bivoca.data.model.GoogleChallengeResponse;
import com.example.bivoca.data.model.LoginResponse;
import com.example.bivoca.data.repository.AuthRepository;
import com.example.bivoca.data.repository.ProfileRepository;
import com.example.bivoca.data.repository.LeaderboardRepository;
import com.example.bivoca.data.repository.LearningRepository;
import com.example.bivoca.data.model.LeaderboardUser;
import com.example.bivoca.data.model.LearningHistory;
import com.example.bivoca.data.model.LearningGoal;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class ProfileViewModel extends AndroidViewModel {

    private final ProfileRepository repository;
    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<User> user = new MutableLiveData<>();
    private final MutableLiveData<GamificationStats> stats = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> googleLinkLoading = new MutableLiveData<>(false);
    private final MutableLiveData<GoogleChallengeResponse> googleLinkPrompt = new MutableLiveData<>();
    private final MutableLiveData<LoginResponse> googleLinkResult = new MutableLiveData<>();
    private final MutableLiveData<String> googleLinkError = new MutableLiveData<>();
    private GoogleChallengeResponse pendingGoogleChallenge;
    private String pendingPassword;

    @Nullable private Call<User> activeUserCall;
    @Nullable private Call<GamificationStats> activeStatsCall;
    private int requestVersion = 0;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ProfileRepository(application.getApplicationContext());
        this.authRepository = AuthRepository.authenticated(application.getApplicationContext());
    }

    // Getters
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<User> getUser() { return user; }
    public LiveData<GamificationStats> getStats() { return stats; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getGoogleLinkLoading() { return googleLinkLoading; }
    public LiveData<GoogleChallengeResponse> getGoogleLinkPrompt() { return googleLinkPrompt; }
    public LiveData<LoginResponse> getGoogleLinkResult() { return googleLinkResult; }
    public LiveData<String> getGoogleLinkError() { return googleLinkError; }

    // Profile-specific requests. Home continues using the original loadProfile path below.
    private final MutableLiveData<ProfileOverviewState> overview = new MutableLiveData<>();
    private final List<Call<?>> overviewCalls = new ArrayList<>();
    private int overviewVersion;
    private LeaderboardRepository overviewLeaderboard;
    private LearningRepository overviewLearning;

    public LiveData<ProfileOverviewState> getOverview() { return overview; }
    private final MutableLiveData<Boolean> avatarSaving = new MutableLiveData<>(false);
    public LiveData<Boolean> getAvatarSaving() { return avatarSaving; }
    public void setAvatarSaving(boolean saving) { avatarSaving.setValue(saving); }

    public void loadOverview() {
        cancelOverview();
        int version = ++overviewVersion;
        if (overviewLeaderboard == null) overviewLeaderboard = new LeaderboardRepository(getApplication());
        if (overviewLearning == null) overviewLearning = new LearningRepository(getApplication());
        ProfileOverviewState previous = overview.getValue();
        overview.setValue((previous == null ? ProfileOverviewState.empty() : previous).loading());
        overviewCalls.add(repository.getUserProfile(new ProfileRepository.UserCallback() {
            @Override public void onSuccess(User value) { if(version==overviewVersion) overview.setValue(overview.getValue().withUser(ProfileOverviewState.Section.success(value))); }
            @Override public void onError(String message) { if(version==overviewVersion) overview.setValue(overview.getValue().withUser(ProfileOverviewState.Section.failure(message,overview.getValue().user))); }
        }));
        overviewCalls.add(repository.getGamificationStats(new ProfileRepository.StatsCallback() {
            @Override public void onSuccess(GamificationStats value) { if(version==overviewVersion) overview.setValue(overview.getValue().withStats(ProfileOverviewState.Section.success(value))); }
            @Override public void onError(String message) { if(version==overviewVersion) overview.setValue(overview.getValue().withStats(ProfileOverviewState.Section.failure(message,overview.getValue().stats))); }
        }));
        overviewCalls.add(overviewLeaderboard.getLeaderboard(100,new LeaderboardRepository.LeaderboardCallback() {
            @Override public void onSuccess(List<LeaderboardUser> value) { if(version==overviewVersion) overview.setValue(overview.getValue().withRank(ProfileOverviewState.Section.success(value))); }
            @Override public void onError(String message) { if(version==overviewVersion) overview.setValue(overview.getValue().withRank(ProfileOverviewState.Section.failure(message,overview.getValue().rank))); }
        }));
        overviewCalls.add(overviewLearning.getHistory(new LearningRepository.Result<LearningHistory>() {
            @Override public void onSuccess(LearningHistory value) { if(version==overviewVersion) overview.setValue(overview.getValue().withWeek(ProfileOverviewState.Section.success(value))); }
            @Override public void onError(String message) { if(version==overviewVersion) overview.setValue(overview.getValue().withWeek(ProfileOverviewState.Section.failure(message,overview.getValue().week))); }
        }));
        overviewCalls.add(overviewLearning.getGoal(new LearningRepository.Result<LearningGoal>() {
            @Override public void onSuccess(LearningGoal value) { if(version==overviewVersion) overview.setValue(overview.getValue().withGoal(ProfileOverviewState.Section.success(value))); }
            @Override public void onError(String message) { if(version==overviewVersion) overview.setValue(overview.getValue().withGoal(ProfileOverviewState.Section.failure(message,overview.getValue().goal))); }
        }));
    }

    private void cancelOverview() {
        for (Call<?> call : overviewCalls) call.cancel();
        overviewCalls.clear();
    }

    public void startGoogleLink(String password) {
        if (Boolean.TRUE.equals(googleLinkLoading.getValue())) return;
        if (password == null || password.isEmpty()) {
            googleLinkError.setValue("Vui lòng nhập mật khẩu BiVoca."); return;
        }
        googleLinkLoading.setValue(true);
        googleLinkError.setValue(null);
        pendingPassword = password;
        authRepository.requestGoogleChallenge(new AuthRepository.ResultCallback<GoogleChallengeResponse>() {
            @Override public void onSuccess(GoogleChallengeResponse value) {
                pendingGoogleChallenge = value; googleLinkPrompt.setValue(value);
            }
            @Override public void onError(String message) {
                clearPendingLink(); googleLinkError.setValue(message);
            }
        });
    }

    public void consumeGoogleLinkPrompt() { googleLinkPrompt.setValue(null); }

    public void completeGoogleLink(String idToken) {
        if (pendingGoogleChallenge == null || pendingPassword == null) {
            cancelGoogleLink(); return;
        }
        authRepository.linkGoogle(idToken, pendingGoogleChallenge, pendingPassword,
                new AuthRepository.LoginCallback() {
                    @Override public void onSuccess(LoginResponse response) {
                        clearPendingLink(); googleLinkResult.setValue(response);
                    }
                    @Override public void onError(String message) {
                        clearPendingLink(); googleLinkError.setValue(message);
                    }
                });
    }

    public void cancelGoogleLink() { clearPendingLink(); }

    private void clearPendingLink() {
        pendingGoogleChallenge = null; pendingPassword = null; googleLinkLoading.setValue(false);
    }

    public void loadProfile() {
        cancelCalls();
        int version = ++requestVersion;

        loading.setValue(true);
        error.setValue(null);

        // Tải thông tin người dùng
        activeUserCall = repository.getUserProfile(new ProfileRepository.UserCallback() {
            @Override
            public void onSuccess(User result) {
                if (version != requestVersion) return;
                user.setValue(result);
                checkLoadingComplete(version);
            }

            @Override
            public void onError(String message) {
                if (version != requestVersion) return;
                error.setValue(message);
                checkLoadingComplete(version);
            }
        });

        // Tải thông tin gamification
        activeStatsCall = repository.getGamificationStats(new ProfileRepository.StatsCallback() {
            @Override
            public void onSuccess(GamificationStats result) {
                if (version != requestVersion) return;
                stats.setValue(result);
                checkLoadingComplete(version);
            }

            @Override
            public void onError(String message) {
                if (version != requestVersion) return;
                // Nếu chưa có lỗi user thì hiển thị lỗi stats
                if (error.getValue() == null) {
                    error.setValue(message);
                }
                checkLoadingComplete(version);
            }
        });
    }

    private void checkLoadingComplete(int version) {
        if (version != requestVersion) return;
        boolean userDone = (user.getValue() != null || error.getValue() != null);
        boolean statsDone = (stats.getValue() != null || error.getValue() != null);
        if (userDone && statsDone) {
            loading.setValue(false);
        }
    }

    private void cancelCalls() {
        if (activeUserCall != null) {
            activeUserCall.cancel();
            activeUserCall = null;
        }
        if (activeStatsCall != null) {
            activeStatsCall.cancel();
            activeStatsCall = null;
        }
    }

    @Override
    protected void onCleared() {
        overviewVersion++;
        cancelOverview();
        requestVersion++;
        cancelCalls();
    }
}
