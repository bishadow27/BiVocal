package com.example.bivoca.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.bivoca.data.model.Topic;

@Entity(tableName = "cached_topics", indices = {@Index(value = "code", unique = true)})
public final class TopicEntity {
    @PrimaryKey public final int id;
    @NonNull public final String code;
    @NonNull public final String name;
    @Nullable public final String description;
    @Nullable public final String imageUrl;
    public final int flashcardCount;
    public final int position;

    public TopicEntity(int id, @NonNull String code, @NonNull String name,
                       @Nullable String description, @Nullable String imageUrl,
                       int flashcardCount, int position) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.flashcardCount = flashcardCount;
        this.position = position;
    }

    public static TopicEntity fromModel(Topic topic, int position) {
        return new TopicEntity(topic.getId(), topic.getCode(), topic.getName(),
                topic.getDescription(), topic.getImageUrl(), topic.getFlashcardCount(), position);
    }

    public Topic toModel() {
        return new Topic(id, code, name, description, imageUrl, flashcardCount);
    }
}
