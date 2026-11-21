package com.example.bivoca.data.model;

import java.util.ArrayList;
import java.util.List;

/** Session-v2 wire DTOs. Option identity and grades belong exclusively to Backend. */
public final class QuizSessionDtos {
    private QuizSessionDtos() { }
    public static class Filter { public String topicCode, posCode; }
    public static class Availability {
        public int vocabularyCount, quizEligibleCount;
        public String unit, availabilityVersion;
        public List<Integer> enabledCounts = new ArrayList<>();
        public Integer fallbackUnitCount;
    }
    public static class Create {
        public String creationId, method, availabilityVersion;
        public Filter filter = new Filter();
        public int selectedUnitCount;
        public boolean useAllAvailable;
    }
    public static class Creation { public String sessionId; public int version; }
    public static class Option { public String optionId, text; }
    public static class Question {
        public String sessionId, questionId, kind, prompt;
        public int ordinal;
        public Integer roundIndex, roundPosition;
        public List<Option> options = new ArrayList<>();
        public boolean answered;
    }
    public static class Answer {
        public String actionId, questionId, selectedOptionId;
        public int expectedVersion;
    }
    public static class Control { public String actionId; public int expectedVersion; }
    public static class Source { public String example, exampleVi; }
    public static class Receipt {
        public String receiptId, actionId, sessionId, questionId, operation;
        public String sessionState, finishReason, answeredAt, acknowledgedAt;
        public boolean acknowledged, correct;
        public Option correctAnswer, selectedAnswer;
        public int awardedXp, sessionVersion;
        public Source source;
    }
    public static class Session {
        public String sessionId, method, unit, state, finishReason, startedAt, finishedAt;
        public int version, selectedUnitCount, answeredCount, correctCount, awardedXp;
        public List<Receipt> receipts = new ArrayList<>();
    }
}
