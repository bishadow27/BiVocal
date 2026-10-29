package com.example.bivoca.data.model;

import com.google.gson.annotations.SerializedName;

public final class ReviewSchedule {
    @SerializedName("trackedCards") private int trackedCards;
    @SerializedName("reviewedCards") private int reviewedCards;
    @SerializedName("dueNow") private int dueNow;
    @SerializedName("upcoming24Hours") private int upcoming24Hours;
    @SerializedName("reviewedToday") private int reviewedToday;
    @SerializedName("memoryLevels") private int[] memoryLevels;

    public int getTrackedCards() { return trackedCards; }
    public int getReviewedCards() { return reviewedCards; }
    public int getDueNow() { return dueNow; }
    public int getUpcoming24Hours() { return upcoming24Hours; }
    public int getReviewedToday() { return reviewedToday; }
    public int[] getMemoryLevels() { return memoryLevels == null ? new int[0] : memoryLevels.clone(); }
}
