package com.example.bivoca.data.model;

import com.google.gson.annotations.SerializedName;

/** DTO trả về từ GET /gamification/me */
public class GamificationStats {
    @SerializedName("totalXp") private int totalXp;
    @SerializedName("level") private int level;
    @SerializedName("xpIntoLevel") private int xpIntoLevel;
    @SerializedName("xpPerLevel") private int xpPerLevel;
    @SerializedName("currentStreak") private int currentStreak;
    @SerializedName("longestStreak") private int longestStreak;
    @SerializedName("lastActivityDate") private String lastActivityDate;

    public int getTotalXp() { return totalXp; }
    public int getLevel() { return level; }
    public int getXpIntoLevel() { return xpIntoLevel; }
    public int getXpPerLevel() { return xpPerLevel; }
    public int getCurrentStreak() { return currentStreak; }
    public int getLongestStreak() { return longestStreak; }
    public String getLastActivityDate() { return lastActivityDate; }
}
