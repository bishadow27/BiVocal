package com.example.bivoca.ui.quiz;

/** Values displayed by the final Quiz result screen. */
public final class QuizResultSummary {
    private final int total;
    private final int correct;
    private final int wrong;
    private final int accuracyPercent;

    private QuizResultSummary(int total, int correct, int wrong, int accuracyPercent) {
        this.total = total;
        this.correct = correct;
        this.wrong = wrong;
        this.accuracyPercent = accuracyPercent;
    }

    public static QuizResultSummary from(int correct, int total) {
        if (total < 0 || correct < 0 || correct > total) {
            throw new IllegalArgumentException("Invalid Quiz counters");
        }
        int accuracy = total == 0 ? 0 : (int) Math.round(correct * 100.0 / total);
        return new QuizResultSummary(total, correct, total - correct, accuracy);
    }

    public int getTotal() { return total; }
    public int getCorrect() { return correct; }
    public int getWrong() { return wrong; }
    public int getAccuracyPercent() { return accuracyPercent; }
}
