package com.example.bivoca.data.local;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.example.bivoca.data.model.Flashcard;
import com.example.bivoca.data.model.Topic;

import java.util.ArrayList;
import java.util.List;

/** Lazily opens Room when the repository's worker reads/writes, never in its constructor. */
public final class RoomLearningCache implements LearningCache {
    private final Context applicationContext;
    @Nullable private final LearningDatabase testDatabase;

    public RoomLearningCache(Context context) {
        applicationContext = context.getApplicationContext();
        testDatabase = null;
    }

    RoomLearningCache(LearningDatabase database) {
        applicationContext = null;
        testDatabase = database;
    }

    private LearningCacheDao dao() {
        return (testDatabase != null ? testDatabase
                : LearningDatabase.getInstance(applicationContext)).learningCacheDao();
    }

    @Override @WorkerThread @Nullable
    public List<Topic> readTopics() {
        List<TopicEntity> rows = dao().readTopics();
        if (rows == null) return null;
        List<Topic> topics = new ArrayList<>(rows.size());
        for (TopicEntity row : rows) topics.add(row.toModel());
        return topics;
    }

    @Override @WorkerThread
    public void writeTopics(List<Topic> topics) {
        List<TopicEntity> rows = new ArrayList<>(topics.size());
        for (int i = 0; i < topics.size(); i++) rows.add(TopicEntity.fromModel(topics.get(i), i));
        dao().replaceTopics(rows, System.currentTimeMillis());
    }

    @Override @WorkerThread @Nullable
    public List<Flashcard> readFlashcards(String topicCode) {
        List<FlashcardEntity> rows = dao().readFlashcards(topicCode);
        if (rows == null) return null;
        List<Flashcard> flashcards = new ArrayList<>(rows.size());
        for (FlashcardEntity row : rows) flashcards.add(row.toModel());
        return flashcards;
    }

    @Override @WorkerThread
    public void writeFlashcards(String topicCode, List<Flashcard> flashcards) {
        List<FlashcardEntity> rows = new ArrayList<>(flashcards.size());
        for (int i = 0; i < flashcards.size(); i++) {
            rows.add(FlashcardEntity.fromModel(topicCode, flashcards.get(i), i));
        }
        dao().replaceFlashcards(topicCode, rows, System.currentTimeMillis());
    }
}
