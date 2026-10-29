package com.example.bivoca.ui.quiz;

/** Pure decision used by both the toolbar and Android system Back behavior. */
public final class QuizBackPolicy {
    public enum Action { LEAVE, CONFIRM_EXIT, BLOCK_WHILE_SUBMITTING }

    private QuizBackPolicy() { }

    public static Action decide(QuizViewModel.QuizState state, int answeredCount) {
        if (state == QuizViewModel.QuizState.SUBMITTING) {
            return Action.BLOCK_WHILE_SUBMITTING;
        }
        if (answeredCount > 0 && state != QuizViewModel.QuizState.FINISHED) {
            return Action.CONFIRM_EXIT;
        }
        return Action.LEAVE;
    }
}
