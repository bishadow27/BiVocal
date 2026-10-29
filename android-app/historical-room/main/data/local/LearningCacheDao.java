package com.example.bivoca.data.local;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public abstract class LearningCacheDao {
    private static final String TOPICS_SCOPE = "topics";

    @Query("SELECT syncedAt FROM cache_sync WHERE scope = :scope")
    @Nullable public abstract Long getSyncedAt(String scope);

    @Query("SELECT * FROM cached_topics ORDER BY position")
    public abstract List<TopicEntity> getTopicRows();

    @Query("SELECT * FROM cached_flashcards WHERE topicCode = :topicCode ORDER BY position")
    public abstract List<FlashcardEntity> getFlashcardRows(String topicCode);

    @Query("DELETE FROM cached_topics")
    public abstract void deleteTopics();

    @Query("DELETE FROM cached_flashcards WHERE topicCode = :topicCode")
    public abstract void deleteFlashcards(String topicCode);

    @Query("SELECT COUNT(*) FROM cached_topics")
    public abstract int countTopics();

    @Query("SELECT COUNT(*) FROM cached_flashcards")
    public abstract int countFlashcards();

    @Query("DELETE FROM cached_flashcards")
    abstract void deleteAllFlashcards();

    @Query("DELETE FROM cache_sync")
    abstract void deleteAllSyncMarkers();

    // ABORT makes duplicate/invalid rows fail the entire replacement transaction.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    public abstract void insertTopics(List<TopicEntity> topics);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public abstract void insertFlashcards(List<FlashcardEntity> flashcards);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void putSync(CacheSyncEntity marker);

    @Transaction
    @Nullable public List<TopicEntity> readTopics() {
        return getSyncedAt(TOPICS_SCOPE) == null ? null : getTopicRows();
    }

    @Transaction
    @Nullable public List<FlashcardEntity> readFlashcards(String topicCode) {
        return getSyncedAt(flashcardScope(topicCode)) == null ? null : getFlashcardRows(topicCode);
    }

    @Transaction
    public void replaceTopics(List<TopicEntity> topics, long syncedAt) {
        deleteTopics();
        insertTopics(topics);
        putSync(new CacheSyncEntity(TOPICS_SCOPE, syncedAt));
    }

    @Transaction
    public void replaceFlashcards(String topicCode, List<FlashcardEntity> flashcards, long syncedAt) {
        deleteFlashcards(topicCode);
        insertFlashcards(flashcards);
        putSync(new CacheSyncEntity(flashcardScope(topicCode), syncedAt));
    }

    /** The database contains public cache only; this preserves app prefs, session and avatar files. */
    @Transaction
    public void clearPublicCache() {
        deleteAllFlashcards();
        deleteTopics();
        deleteAllSyncMarkers();
    }

    private static String flashcardScope(String topicCode) {
        return "flashcards/" + topicCode;
    }
}
