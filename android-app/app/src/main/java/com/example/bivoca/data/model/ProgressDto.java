package com.example.bivoca.data.model;

import com.google.gson.annotations.SerializedName;

public class ProgressDto {
    @SerializedName("flashcardId") private int flashcardId;
    @SerializedName("memoryLevel") private int memoryLevel;
    @SerializedName("reviewCount") private int reviewCount;
    @SerializedName("correctCount") private int correctCount;
    @SerializedName("wrongCount") private int wrongCount;
    @SerializedName("isFavorite") private boolean isFavorite;
    @SerializedName("lastReview") private String lastReview;
    @SerializedName("nextReview") private String nextReview;

    public int getFlashcardId() { return flashcardId; }
    public int getMemoryLevel() { return memoryLevel; }
    public int getReviewCount() { return reviewCount; }
    public int getCorrectCount() { return correctCount; }
    public int getWrongCount() { return wrongCount; }
    public boolean isFavorite() { return isFavorite; }
    public String getLastReview() { return lastReview; }
    public String getNextReview() { return nextReview; }
}
