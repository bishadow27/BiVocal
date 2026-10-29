package com.example.bivoca.data.model;
import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
public final class LearningDay {
    @SerializedName("date") private String date; @SerializedName("goal") @Nullable private Integer goal;
    @SerializedName("newWords") private int newWords; @SerializedName("reviews") private int reviews;
    @SerializedName("status") private String status;
    public String getDate(){return date;} @Nullable public Integer getGoal(){return goal;}
    public int getNewWords(){return newWords;} public int getReviews(){return reviews;}
    public String getStatus(){return status;}
}
