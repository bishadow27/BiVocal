package com.example.bivoca.data.model;

import com.google.gson.Gson;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProgressSummaryTest {

    @Test
    public void testProgressSummaryDeserialization() {
        String json = "{\n" +
                "  \"trackedCards\": 50,\n" +
                "  \"reviewedCards\": 35,\n" +
                "  \"favoriteCards\": 12,\n" +
                "  \"masteredCards\": 18,\n" +
                "  \"totalReviews\": 120,\n" +
                "  \"correctAnswers\": 96,\n" +
                "  \"wrongAnswers\": 24,\n" +
                "  \"accuracy\": 80.0\n" +
                "}";

        Gson gson = new Gson();
        ProgressSummary summary = gson.fromJson(json, ProgressSummary.class);

        assertNotNull(summary);
        assertEquals(50, summary.getTrackedCards());
        assertEquals(35, summary.getReviewedCards());
        assertEquals(12, summary.getFavoriteCards());
        assertEquals(18, summary.getMasteredCards());
        assertEquals(120, summary.getTotalReviews());
        assertEquals(96, summary.getCorrectAnswers());
        assertEquals(24, summary.getWrongAnswers());
        assertNotNull(summary.getAccuracy());
        assertEquals(80.0, summary.getAccuracy(), 0.001);
    }
}
