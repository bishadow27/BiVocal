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

    @GET("quiz/availability")
    Call<com.example.bivoca.data.model.QuizSessionDtos.Availability> availability(
            @Query("method") String method,@Query("topicCode") String topic,@Query("posCode") String pos);
    @POST("quiz/sessions")
    Call<com.example.bivoca.data.model.QuizSessionDtos.Creation> createSession(@Body com.example.bivoca.data.model.QuizSessionDtos.Create request);
    @GET("quiz/sessions/{id}")
    Call<com.example.bivoca.data.model.QuizSessionDtos.Session> resumeSession(@retrofit2.http.Path("id") String id);
    @GET("quiz/sessions/{id}/questions")
    Call<List<com.example.bivoca.data.model.QuizSessionDtos.Question>> sessionQuestions(@retrofit2.http.Path("id") String id,@Query("cursor") int cursor,@Query("limit") int limit);
    @POST("quiz/sessions/{id}/answers")
    Call<com.example.bivoca.data.model.QuizSessionDtos.Receipt> answerSession(@retrofit2.http.Path("id") String id,@Body com.example.bivoca.data.model.QuizSessionDtos.Answer request);
    @POST("quiz/sessions/{id}/complete")
    Call<com.example.bivoca.data.model.QuizSessionDtos.Receipt> completeSession(@retrofit2.http.Path("id") String id,@Body com.example.bivoca.data.model.QuizSessionDtos.Control request);
    @POST("quiz/sessions/{id}/abandon")
    Call<com.example.bivoca.data.model.QuizSessionDtos.Receipt> abandonSession(@retrofit2.http.Path("id") String id,@Body com.example.bivoca.data.model.QuizSessionDtos.Control request);

}
