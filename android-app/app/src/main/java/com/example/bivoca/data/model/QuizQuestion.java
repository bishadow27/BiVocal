package com.example.bivoca.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Một câu hỏi quiz trả về từ GET /quiz/questions */
public class QuizQuestion {
    @SerializedName("flashcardId") private int flashcardId;
    @SerializedName("questionType") private String questionType;
    @SerializedName("prompt") private String prompt;
    @SerializedName("options") private List<QuizOption> options;

    public int getFlashcardId() { return flashcardId; }
    public String getQuestionType() { return questionType; }
    public String getPrompt() { return prompt; }
    public List<QuizOption> getOptions() { return options; }
}
