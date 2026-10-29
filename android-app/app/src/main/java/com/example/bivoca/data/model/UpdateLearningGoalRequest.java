package com.example.bivoca.data.model;
import com.google.gson.annotations.SerializedName;
public final class UpdateLearningGoalRequest {
    @SerializedName("dailyGoal") private final int dailyGoal;
    public UpdateLearningGoalRequest(int dailyGoal) { this.dailyGoal=dailyGoal; }
}
