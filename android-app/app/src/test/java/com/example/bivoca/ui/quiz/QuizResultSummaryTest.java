package com.example.bivoca.ui.quiz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class QuizResultSummaryTest {
    @Test
    public void calculatesTotalsWrongAndRoundedAccuracy() {
        QuizResultSummary result = QuizResultSummary.from(7, 10);
        assertEquals(10, result.getTotal());
        assertEquals(7, result.getCorrect());
        assertEquals(3, result.getWrong());
        assertEquals(70, result.getAccuracyPercent());

        assertEquals(67, QuizResultSummary.from(2, 3).getAccuracyPercent());
        assertEquals(0, QuizResultSummary.from(0, 0).getAccuracyPercent());
    }

    @Test
    public void rejectsImpossibleCounters() {
        assertThrows(IllegalArgumentException.class, () -> QuizResultSummary.from(4, 3));
        assertThrows(IllegalArgumentException.class, () -> QuizResultSummary.from(-1, 3));
        assertThrows(IllegalArgumentException.class, () -> QuizResultSummary.from(0, -1));
    }
}
