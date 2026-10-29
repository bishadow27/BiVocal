package com.example.bivoca.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;

import com.example.bivoca.data.model.Flashcard;

/** A per-topic response snapshot; replacing one topic never removes another. */
@Entity(tableName = "cached_flashcards", primaryKeys = {"topicCode", "id"})
public final class FlashcardEntity {
    @NonNull public final String topicCode;
    public final int id;
    @NonNull public final String english;
    @NonNull public final String vietnamese;
    @Nullable public final String pronunciation;
    @Nullable public final String partOfSpeech;
    @Nullable public final String example;
    @Nullable public final String exampleVi;
    @Nullable public final String level;
    public final int position;

    public FlashcardEntity(@NonNull String topicCode, int id, @NonNull String english,
                           @NonNull String vietnamese, @Nullable String pronunciation,
                           @Nullable String partOfSpeech, @Nullable String example,
                           @Nullable String exampleVi, @Nullable String level, int position) {
        this.topicCode = topicCode;
        this.id = id;
        this.english = english;
        this.vietnamese = vietnamese;
        this.pronunciation = pronunciation;
        this.partOfSpeech = partOfSpeech;
        this.example = example;
        this.exampleVi = exampleVi;
        this.level = level;
        this.position = position;
    }

    public static FlashcardEntity fromModel(String topicCode, Flashcard card, int position) {
        return new FlashcardEntity(topicCode, card.getId(), card.getEnglish(), card.getVietnamese(),
                card.getPronunciation(), card.getPartOfSpeech(), card.getExample(),
                card.getExampleVi(), card.getLevel(), position);
    }

    public Flashcard toModel() {
        return new Flashcard(id, english, vietnamese, pronunciation,
                partOfSpeech, example, exampleVi, level);
    }
}
