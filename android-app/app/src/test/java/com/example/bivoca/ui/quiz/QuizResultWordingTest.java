package com.example.bivoca.ui.quiz;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** Pure result-label assertions; no Activity, repository, network or database. */
public class QuizResultWordingTest {
    @Test public void matchingReviewUsesPairUnit() {
        assertEquals("Xem chi tiết bài làm (2 cặp sai) ▾",
                QuizSessionScreen.ResultWording.collapsedReviewLabel("MATCHING", 2));
    }

    @Test public void ordinaryQuizReviewKeepsQuestionUnit() {
        assertEquals("Xem chi tiết bài làm (1 câu sai) ▾",
                QuizSessionScreen.ResultWording.collapsedReviewLabel("EN_TO_VI", 1));
    }
    @Test public void emptyMatchingReviewUsesPairUnit(){assertEquals("Chưa có cặp sai trong phiên này.",QuizSessionScreen.ResultWording.emptyReviewLabel("MATCHING"));}
    @Test public void emptyOrdinaryReviewKeepsExistingWording(){assertEquals("Chưa có câu trả lời sai trong phiên này.",QuizSessionScreen.ResultWording.emptyReviewLabel("EN_TO_VI"));}
}
