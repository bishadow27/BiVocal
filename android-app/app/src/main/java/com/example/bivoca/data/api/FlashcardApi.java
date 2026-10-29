package com.example.bivoca.data.api;

import com.example.bivoca.data.model.Flashcard;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface FlashcardApi {
    // Public endpoint: no query parameters and no Authorization header required.
    @GET("topics/{code}/flashcards")
    Call<List<Flashcard>> getFlashcards(@Path("code") String topicCode);
}
