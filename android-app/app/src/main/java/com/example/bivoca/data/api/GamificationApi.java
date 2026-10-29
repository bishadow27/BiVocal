package com.example.bivoca.data.api;

import com.example.bivoca.data.model.GamificationStats;
import com.example.bivoca.data.model.LeaderboardUser;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GamificationApi {

    @GET("gamification/me")
    Call<GamificationStats> getMyStats();

    @GET("gamification/leaderboard")
    Call<List<LeaderboardUser>> getLeaderboard(@Query("limit") Integer limit);

    @GET("gamification/leaderboard")
    Call<List<LeaderboardUser>> getLeaderboard();
}
