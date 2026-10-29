package com.example.bivoca.data.local;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.example.bivoca.data.model.Flashcard;
import com.example.bivoca.data.model.Topic;

import java.util.List;

/** Public API snapshots only. All calls belong on a worker executor. */
public interface LearningCache {
    /** null: never downloaded; empty: a successful empty response was saved. */
    @WorkerThread @Nullable List<Topic> readTopics();
    @WorkerThread void writeTopics(List<Topic> topics);

    /** Each topic has its own snapshot and successful-download marker. */
    @WorkerThread @Nullable List<Flashcard> readFlashcards(String topicCode);
    @WorkerThread void writeFlashcards(String topicCode, List<Flashcard> flashcards);
}
