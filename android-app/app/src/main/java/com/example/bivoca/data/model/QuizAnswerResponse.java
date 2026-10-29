package com.example.bivoca.data.model;

import com.google.gson.annotations.SerializedName;

/** Response từ POST /quiz/answer — QuizAnswerDto */
public class QuizAnswerResponse {
    @SerializedName("resultId") private int resultId;
    @SerializedName("flashcardId") private int flashcardId;
    @SerializedName("selectedFlashcardId") private int selectedFlashcardId;
    @SerializedName("correct") private boolean correct;
    @SerializedName("correctAnswer") private String correctAnswer;
    @SerializedName("selectedAnswer") private String selectedAnswer;
    @SerializedName("answeredAt") private String answeredAt;

    public int getResultId() { return resultId; }
    public int getFlashcardId() { return flashcardId; }
    public int getSelectedFlashcardId() { return selectedFlashcardId; }
    public boolean isCorrect() { return correct; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getSelectedAnswer() { return selectedAnswer; }
    public String getAnsweredAt() { return answeredAt; }
}
