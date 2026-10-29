package com.example.bivoca.data.model;

import com.google.gson.annotations.SerializedName;

/** Một lựa chọn trong câu hỏi quiz (EN_TO_VI) */
public class QuizOption {
    @SerializedName("flashcardId") private int flashcardId;
    @SerializedName("text") private String text;

    public int getFlashcardId() { return flashcardId; }
    public String getText() { return text; }
}
