package com.example.bivoca.data.model;

public class Topic {

    private int id;
    private String code;
    private String name;
    private String description;
    private String imageUrl;
    private int flashcardCount;
    private String topicGroup;

    public Topic() { }

    /** Maps the five fields supplied by the public topics endpoint. */
    public Topic(int id, String code, String name, String description, String imageUrl) {
        this(id, code, name, description, imageUrl, 0);
    }

    public Topic(int id, String code, String name, String description, String imageUrl,
                 int flashcardCount) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.flashcardCount = flashcardCount;
    }

    public int getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getFlashcardCount() {
        return flashcardCount;
    }

    public String getTopicGroup() { return topicGroup; }
    public boolean isPartOfSpeechGroup() { return "PART_OF_SPEECH".equals(topicGroup); }
}
