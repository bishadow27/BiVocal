package com.example.bivoca.ui.progress;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bivoca.data.model.ProgressSummary;
import com.example.bivoca.data.model.ReviewSchedule;
import com.example.bivoca.data.model.LearningToday;
import com.example.bivoca.data.model.LearningHistory;
import com.example.bivoca.data.repository.ProgressRepository;
import com.example.bivoca.data.repository.LearningRepository;

import retrofit2.Call;

public class ProgressViewModel extends AndroidViewModel {

    private final ProgressRepository repository;
    private final LearningRepository learningRepository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<ProgressSummary> summary = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<ReviewSchedule> schedule = new MutableLiveData<>();
    private final MutableLiveData<String> scheduleError = new MutableLiveData<>();
    private final MutableLiveData<LearningToday> learningToday = new MutableLiveData<>();
    private final MutableLiveData<LearningHistory> learningHistory = new MutableLiveData<>();
    private final MutableLiveData<String> learningError = new MutableLiveData<>();

    @Nullable private Call<ProgressSummary> activeCall;
    @Nullable private Call<ReviewSchedule> scheduleCall;
    @Nullable private Call<LearningToday> todayCall;
    @Nullable private Call<LearningHistory> historyCall;
    private int requestVersion = 0;

    public ProgressViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ProgressRepository(application.getApplicationContext());
        this.learningRepository = new LearningRepository(application.getApplicationContext());
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<ProgressSummary> getSummary() { return summary; }
    public LiveData<String> getError() { return error; }
    public LiveData<ReviewSchedule> getSchedule() { return schedule; }
    public LiveData<String> getScheduleError() { return scheduleError; }
    public LiveData<LearningToday> getLearningToday() { return learningToday; }
    public LiveData<LearningHistory> getLearningHistory() { return learningHistory; }
    public LiveData<String> getLearningError() { return learningError; }

    public void loadSummary() {
        cancelCall();
        int version = ++requestVersion;

        loading.setValue(true);
        error.setValue(null);
        scheduleError.setValue(null);
        schedule.setValue(null);
        learningError.setValue(null);

        todayCall = learningRepository.getToday(new LearningRepository.Result<LearningToday>() {
            @Override public void onSuccess(LearningToday value) { if (version == requestVersion) learningToday.setValue(value); }
            @Override public void onError(String message) { if (version == requestVersion) learningError.setValue(message); }
        });
        historyCall = learningRepository.getHistory(new LearningRepository.Result<LearningHistory>() {
            @Override public void onSuccess(LearningHistory value) { if (version == requestVersion) learningHistory.setValue(value); }
            @Override public void onError(String message) { if (version == requestVersion) learningError.setValue(message); }
        });

        scheduleCall = repository.getSchedule(new ProgressRepository.ScheduleCallback() {
            @Override public void onSuccess(ReviewSchedule result) {
                if (version != requestVersion) return;
                schedule.setValue(result);
            }
            @Override public void onError(String message) {
                if (version != requestVersion) return;
                scheduleError.setValue(message);
            }
        });

        activeCall = repository.getSummary(new ProgressRepository.SummaryCallback() {
            @Override
            public void onSuccess(ProgressSummary result) {
                if (version != requestVersion) return;
                loading.setValue(false);
                summary.setValue(result);
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
        if (scheduleCall != null) {
            scheduleCall.cancel();
            scheduleCall = null;
        }
        if (todayCall != null) { todayCall.cancel(); todayCall = null; }
        if (historyCall != null) { historyCall.cancel(); historyCall = null; }
    }

    @Override
    protected void onCleared() {
        requestVersion++;
        cancelCall();
    }
}
