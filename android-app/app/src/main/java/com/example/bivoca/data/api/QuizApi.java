package com.example.bivoca.data.api;

import com.example.bivoca.data.model.QuizAnswerRequest;
import com.example.bivoca.data.model.QuizAnswerResponse;
import com.example.bivoca.data.model.QuizQuestion;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface QuizApi {
    /**
     * Lấy danh sách câu hỏi quiz.
     * Yêu cầu JWT. topicCode tùy chọn, limit mặc định 10 (tối đa 20).
     */
    @GET("quiz/questions")
    Call<List<QuizQuestion>> getQuestions(
            @Query("topicCode") String topicCode,
            @Query("limit") int limit
    );

    /**
     * Gửi đáp án cho một câu hỏi.
     * Yêu cầu JWT. Body: { flashcardId, selectedFlashcardId }.
     */
    @POST("quiz/answer")
    Call<QuizAnswerResponse> submitAnswer(@Body QuizAnswerRequest request);
}
