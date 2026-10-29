package com.example.bivoca.data.api;

import com.example.bivoca.data.model.FavoriteRequest;
import com.example.bivoca.data.model.ProgressDto;
import com.example.bivoca.data.model.ProgressSummary;
import com.example.bivoca.data.model.ReviewSchedule;
import com.example.bivoca.data.model.Flashcard;
import com.example.bivoca.data.model.ReviewRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;

public interface ProgressApi {

    @GET("progress/summary")
    Call<ProgressSummary> getSummary();

    @GET("progress/schedule")
    Call<ReviewSchedule> getSchedule();

    @GET("progress/due")
    Call<List<Flashcard>> getDueCards(@Query("limit") int limit);

    @GET("progress/{flashcardId}")
    Call<ProgressDto> getProgress(@Path("flashcardId") int flashcardId);

    @POST("progress/{flashcardId}/review")
    Call<ProgressDto> submitReview(
            @Path("flashcardId") int flashcardId,
            @Body ReviewRequest request
    );

    @PUT("progress/{flashcardId}/favorite")
    Call<ProgressDto> setFavorite(
            @Path("flashcardId") int flashcardId,
            @Body FavoriteRequest request
    );
}
