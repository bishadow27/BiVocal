package com.example.bivoca.data.api;

import com.example.bivoca.data.model.FlashcardPage;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface VocabularyApi {
    @GET("flashcards")
    Call<FlashcardPage> search(@Query("search") String search, @Query("topicCode") String topicCode,
                               @Query("level") String level, @Query("page") int page,
                               @Query("limit") int limit);
}
