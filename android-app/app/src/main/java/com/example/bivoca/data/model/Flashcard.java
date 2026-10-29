package com.example.bivoca.data.model;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** Public flashcard DTO, including the M15.5 content-review status. */
public final class Flashcard {
    @SerializedName("id") private int id;
    @SerializedName("english") private String english;
    @SerializedName("vietnamese") private String vietnamese;
    @SerializedName("pronunciation") @Nullable private String pronunciation;
    @SerializedName("partOfSpeech") @Nullable private String partOfSpeech;
    @SerializedName("example") @Nullable private String example;
    @SerializedName("exampleVi") @Nullable private String exampleVi;
    @SerializedName("level") @Nullable private String level;
    @SerializedName("contentStatus") @Nullable private String contentStatus;
    @SerializedName("topicCode") @Nullable private String topicCode;
    @SerializedName("memoryLevel") private int memoryLevel;

    public Flashcard() { }

    public Flashcard(int id, String english, String vietnamese,
                     @Nullable String pronunciation, @Nullable String partOfSpeech,
                     @Nullable String example, @Nullable String exampleVi, @Nullable String level) {
        this.id = id;
        this.english = english;
        this.vietnamese = vietnamese;
        this.pronunciation = pronunciation;
        this.partOfSpeech = partOfSpeech;
        this.example = example;
        this.exampleVi = exampleVi;
        this.level = level;
    }

    public int getId() { return id; }
    public String getEnglish() { return english; }
    public String getVietnamese() { return vietnamese; }
    @Nullable public String getPronunciation() { return pronunciation; }
    @Nullable public String getPartOfSpeech() { return partOfSpeech; }
    @Nullable public String getExample() { return example; }
    @Nullable public String getExampleVi() { return exampleVi; }
    @Nullable public String getLevel() { return level; }
    @Nullable public String getContentStatus() { return contentStatus; }
    public boolean isImportedUnverified() { return "IMPORTED_UNVERIFIED".equals(contentStatus); }
    @Nullable public String getTopicCode() { return topicCode; }
    public int getMemoryLevel() { return memoryLevel; }
}
