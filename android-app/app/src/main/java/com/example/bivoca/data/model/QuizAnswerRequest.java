package com.example.bivoca.data.model;

/** Request body gửi lên POST /quiz/answer */
public class QuizAnswerRequest {
    private final int flashcardId;
    private final int selectedFlashcardId;

    public QuizAnswerRequest(int flashcardId, int selectedFlashcardId) {
        this.flashcardId = flashcardId;
        this.selectedFlashcardId = selectedFlashcardId;
    }

    public int getFlashcardId() { return flashcardId; }
    public int getSelectedFlashcardId() { return selectedFlashcardId; }
}
