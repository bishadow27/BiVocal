package com.example.bivoca.ui.quiz;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class QuizBackPolicyTest {
    @Test public void blocksBackWhileAnswerIsBeingSubmitted() {
        assertEquals(QuizBackPolicy.Action.BLOCK_WHILE_SUBMITTING,
                QuizBackPolicy.decide(QuizViewModel.QuizState.SUBMITTING, 0));
    }

    @Test public void confirmsWhenLeavingAnActiveAttempt() {
        assertEquals(QuizBackPolicy.Action.CONFIRM_EXIT,
                QuizBackPolicy.decide(QuizViewModel.QuizState.QUESTION, 1));
        assertEquals(QuizBackPolicy.Action.CONFIRM_EXIT,
                QuizBackPolicy.decide(QuizViewModel.QuizState.RESULT, 1));
    }

    @Test public void leavesWithoutDialogBeforeAnswerOrAfterCompletion() {
        assertEquals(QuizBackPolicy.Action.LEAVE,
                QuizBackPolicy.decide(QuizViewModel.QuizState.QUESTION, 0));
        assertEquals(QuizBackPolicy.Action.LEAVE,
                QuizBackPolicy.decide(QuizViewModel.QuizState.FINISHED, 10));
    }
}
