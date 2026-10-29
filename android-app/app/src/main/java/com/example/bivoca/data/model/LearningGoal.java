package com.example.bivoca.data.model;
import com.google.gson.annotations.SerializedName;
public final class LearningGoal {
    @SerializedName("dailyGoal") private int dailyGoal;
    public int getDailyGoal() { return dailyGoal; }
}
