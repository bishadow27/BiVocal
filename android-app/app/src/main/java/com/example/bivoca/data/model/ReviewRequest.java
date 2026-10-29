package com.example.bivoca.data.model;

public class ReviewRequest {
    private final boolean correct;

    public ReviewRequest(boolean correct) {
        this.correct = correct;
    }

    public boolean isCorrect() {
        return correct;
    }
}
