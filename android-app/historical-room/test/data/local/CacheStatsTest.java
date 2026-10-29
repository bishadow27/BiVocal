package com.example.bivoca.data.local;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CacheStatsTest {
    @Test public void reportsOnlyProvidedPublicCacheCountsAndClampsNegativeValues() {
        CacheStats stats = new CacheStats(10, 90, 2048);
        assertEquals(10, stats.getTopicCount());
        assertEquals(90, stats.getFlashcardCount());
        assertTrue(stats.formattedSize().contains("KB"));
        CacheStats empty = new CacheStats(-1, -2, -3);
        assertEquals(0, empty.getTopicCount());
        assertEquals(0, empty.getFlashcardCount());
        assertEquals(0L, empty.getBytes());
    }
}
