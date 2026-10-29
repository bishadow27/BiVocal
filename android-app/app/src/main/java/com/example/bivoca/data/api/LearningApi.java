package com.example.bivoca.data.api;
import com.example.bivoca.data.model.*; import java.util.List;
import retrofit2.Call; import retrofit2.http.*;
public interface LearningApi {
 @GET("learning/goal") Call<LearningGoal> getGoal();
 @PUT("learning/goal") Call<LearningGoal> updateGoal(@Body UpdateLearningGoalRequest request);
 @GET("learning/today") Call<LearningToday> getToday();
 @GET("learning/history") Call<LearningHistory> getHistory(@Query("days") int days);
 @GET("learning/new-cards") Call<List<Flashcard>> getNewCards(@Query("limit") int limit);
}
