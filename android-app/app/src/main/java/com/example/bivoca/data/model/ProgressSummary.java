package com.example.bivoca.data.model;

import com.google.gson.annotations.SerializedName;

public class ProgressSummary {
    @SerializedName("trackedCards") private int trackedCards;
    @SerializedName("reviewedCards") private int reviewedCards;
    @SerializedName("favoriteCards") private int favoriteCards;
    @SerializedName("masteredCards") private int masteredCards;
    @SerializedName("totalReviews") private int totalReviews;
    @SerializedName("correctAnswers") private int correctAnswers;
    @SerializedName("wrongAnswers") private int wrongAnswers;
    @SerializedName("accuracy") private Double accuracy;

    public int getTrackedCards() { return trackedCards; }
    public int getReviewedCards() { return reviewedCards; }
    public int getFavoriteCards() { return favoriteCards; }
    public int getMasteredCards() { return masteredCards; }
    public int getTotalReviews() { return totalReviews; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getWrongAnswers() { return wrongAnswers; }
    public Double getAccuracy() { return accuracy; }
}
