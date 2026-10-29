package com.example.bivoca.data.model;
import com.google.gson.annotations.SerializedName;
public final class LearningToday {
    @SerializedName("date") private String date;
    @SerializedName("dailyGoal") private int dailyGoal;
    @SerializedName("newWordsLearned") private int newWordsLearned;
    @SerializedName("remaining") private int remaining;
    @SerializedName("completionPercent") private int completionPercent;
    @SerializedName("reviewCount") private int reviewCount;
    @SerializedName("dueCount") private int dueCount;
    @SerializedName("completed") private boolean completed;
    public String getDate(){return date;} public int getDailyGoal(){return dailyGoal;}
    public int getNewWordsLearned(){return newWordsLearned;} public int getRemaining(){return remaining;}
    public int getCompletionPercent(){return completionPercent;} public int getReviewCount(){return reviewCount;}
    public int getDueCount(){return dueCount;} public boolean isCompleted(){return completed;}
}
