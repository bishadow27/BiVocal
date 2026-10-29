package com.example.bivoca.data.model;
import com.google.gson.annotations.SerializedName;
import java.util.Collections; import java.util.List;
public final class LearningHistory {
    @SerializedName("days") private List<LearningDay> days;
    public List<LearningDay> getDays(){return days==null? Collections.emptyList():days;}
}
