package com.example.bivoca.data.local;

import java.util.Locale;

public final class CacheStats {
    private final int topicCount;
    private final int flashcardCount;
    private final long bytes;

    public CacheStats(int topicCount, int flashcardCount, long bytes) {
        this.topicCount = Math.max(0, topicCount);
        this.flashcardCount = Math.max(0, flashcardCount);
        this.bytes = Math.max(0L, bytes);
    }

    public int getTopicCount() { return topicCount; }
    public int getFlashcardCount() { return flashcardCount; }
    public long getBytes() { return bytes; }

    public String formattedSize() {
        if (bytes < 1024L * 1024L) {
            return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
