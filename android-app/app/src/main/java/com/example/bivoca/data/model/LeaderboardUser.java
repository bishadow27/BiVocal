package com.example.bivoca.data.model;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class LeaderboardUser {

    @SerializedName("rank")
    private int rank;

    @SerializedName("userId")
    private int userId;

    @SerializedName("username")
    private String username;

    @Nullable
    @SerializedName("fullName")
    private String fullName;

    @Nullable
    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("totalXp")
    private int totalXp;

    @SerializedName("level")
    private int level;

    @SerializedName("currentStreak")
    private int currentStreak;

    @SerializedName("longestStreak")
    private int longestStreak;

    public LeaderboardUser() {
    }

    public LeaderboardUser(int rank, int userId, String username, @Nullable String fullName,
                           @Nullable String avatarUrl, int totalXp, int level,
                           int currentStreak, int longestStreak) {
        this.rank = rank;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.totalXp = totalXp;
        this.level = level;
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
    }

    public int getRank() {
        return rank;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username != null ? username : "";
    }

    @Nullable
    public String getFullName() {
        return fullName;
    }

    public String getDisplayName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName;
        }
        return getUsername();
    }

    @Nullable
    public String getAvatarUrl() {
        return avatarUrl;
    }

    public int getTotalXp() {
        return totalXp;
    }

    public int getLevel() {
        return level;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }
}
