package com.example.bivoca.data.local;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.bivoca.data.model.Flashcard;
import com.example.bivoca.data.model.Topic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/** Uses a separate in-memory database and never clears the installed app's data. */
@RunWith(AndroidJUnit4.class)
public class RoomLearningCacheTest {
    private LearningDatabase database;
    private RoomLearningCache cache;

    @Before
    public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                LearningDatabase.class).build();
        cache = new RoomLearningCache(database);
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void neverDownloadedDiffersFromSuccessfullyDownloadedEmptyList() {
        assertNull(cache.readTopics());
        assertNull(cache.readFlashcards("FOOD"));
        cache.writeTopics(Collections.emptyList());
        cache.writeFlashcards("FOOD", Collections.emptyList());

        assertNotNull(cache.readTopics());
        assertTrue(cache.readTopics().isEmpty());
        assertNotNull(cache.readFlashcards("FOOD"));
        assertTrue(cache.readFlashcards("FOOD").isEmpty());
        assertNull(cache.readFlashcards("ANIMALS"));
        assertTrue(database.learningCacheDao().getSyncedAt("topics") > 0);
        assertTrue(database.learningCacheDao().getSyncedAt("flashcards/FOOD") > 0);
    }

    @Test
    public void topicsPreserveApiOrderAndNullableFieldsAndReplaceRemovedRows() {
        Topic food = new Topic(8, "FOOD", "Thức ăn", null, null, 135);
        Topic animals = new Topic(2, "ANIMALS", "Động vật", "Mô tả", "https://example.org/a.png", 30);
        cache.writeTopics(Arrays.asList(food, animals));

        List<Topic> topics = cache.readTopics();
        assertEquals(8, topics.get(0).getId());
        assertEquals("FOOD", topics.get(0).getCode());
        assertEquals(135, topics.get(0).getFlashcardCount());
        assertEquals("Thức ăn", topics.get(0).getName());
        assertNull(topics.get(0).getDescription());
        assertNull(topics.get(0).getImageUrl());
        assertEquals("Mô tả", topics.get(1).getDescription());
        assertEquals("https://example.org/a.png", topics.get(1).getImageUrl());

        cache.writeTopics(Collections.singletonList(animals));
        assertEquals(1, cache.readTopics().size());
        assertEquals("ANIMALS", cache.readTopics().get(0).getCode());
        cache.writeTopics(Collections.emptyList());
        assertTrue(cache.readTopics().isEmpty());
    }

    @Test
    public void flashcardsRoundTripEveryFieldIncludingNullAndKeepOrder() {
        Flashcard apple = new Flashcard(9, "apple", "quả táo", "/ˈæpəl/", "noun",
                "An apple.", "Một quả táo.", "A1");
        Flashcard bread = card(2, "bread");
        cache.writeFlashcards("FOOD", Arrays.asList(apple, bread));

        List<Flashcard> cards = cache.readFlashcards("FOOD");
        assertEquals(9, cards.get(0).getId());
        assertEquals("apple", cards.get(0).getEnglish());
        assertEquals("quả táo", cards.get(0).getVietnamese());
        assertEquals("/ˈæpəl/", cards.get(0).getPronunciation());
        assertEquals("noun", cards.get(0).getPartOfSpeech());
        assertEquals("An apple.", cards.get(0).getExample());
        assertEquals("Một quả táo.", cards.get(0).getExampleVi());
        assertEquals("A1", cards.get(0).getLevel());
        assertEquals(2, cards.get(1).getId());
        assertNull(cards.get(1).getPronunciation());
        assertNull(cards.get(1).getPartOfSpeech());
        assertNull(cards.get(1).getExample());
        assertNull(cards.get(1).getExampleVi());
        assertNull(cards.get(1).getLevel());
    }

    @Test
    public void replacingOneTopicDoesNotMixOrDeleteOtherTopicSnapshots() {
        // Composite keys remain safe even if a future API returns one ID in two topics.
        cache.writeFlashcards("FOOD", Collections.singletonList(card(1, "apple")));
        cache.writeFlashcards("ANIMALS", Collections.singletonList(card(1, "cat")));
        assertEquals("apple", cache.readFlashcards("FOOD").get(0).getEnglish());
        assertEquals("cat", cache.readFlashcards("ANIMALS").get(0).getEnglish());

        cache.writeFlashcards("FOOD", Collections.singletonList(card(2, "bread")));
        assertEquals(1, cache.readFlashcards("FOOD").size());
        assertEquals("bread", cache.readFlashcards("FOOD").get(0).getEnglish());
        cache.writeFlashcards("FOOD", Collections.emptyList());
        assertTrue(cache.readFlashcards("FOOD").isEmpty());
        cache.writeTopics(Collections.emptyList());
        assertEquals("cat", cache.readFlashcards("ANIMALS").get(0).getEnglish());
    }

    @Test
    public void failedReplacementRollsBackRowsAndSyncMarkerTogether() {
        LearningCacheDao dao = database.learningCacheDao();
        FlashcardEntity original = FlashcardEntity.fromModel("FOOD", card(1, "apple"), 0);
        dao.replaceFlashcards("FOOD", Collections.singletonList(original), 100L);
        FlashcardEntity replacement = FlashcardEntity.fromModel("FOOD", card(2, "bread"), 0);

        assertThrows(RuntimeException.class, () -> dao.replaceFlashcards("FOOD",
                Arrays.asList(replacement, replacement), 200L));

        assertEquals(1, cache.readFlashcards("FOOD").size());
        assertEquals("apple", cache.readFlashcards("FOOD").get(0).getEnglish());
        assertEquals(Long.valueOf(100L), dao.getSyncedAt("flashcards/FOOD"));

        TopicEntity topic = TopicEntity.fromModel(new Topic(1, "FOOD", "Food", null, null), 0);
        dao.replaceTopics(Collections.singletonList(topic), 300L);
        assertThrows(RuntimeException.class,
                () -> dao.replaceTopics(Arrays.asList(topic, topic), 400L));
        assertEquals(1, cache.readTopics().size());
        assertEquals(Long.valueOf(300L), dao.getSyncedAt("topics"));
    }

    @Test
    public void clearPublicCacheRemovesOnlyTheThreeRoomCacheTables() {
        cache.writeTopics(Collections.singletonList(new Topic(1, "FOOD", "Food", null, null)));
        cache.writeFlashcards("FOOD", Collections.singletonList(card(1, "apple")));
        LearningCacheDao dao = database.learningCacheDao();
        assertEquals(1, dao.countTopics());
        assertEquals(1, dao.countFlashcards());

        dao.clearPublicCache();

        assertEquals(0, dao.countTopics());
        assertEquals(0, dao.countFlashcards());
        assertNull(cache.readTopics());
        assertNull(cache.readFlashcards("FOOD"));
    }

    private static Flashcard card(int id, String english) {
        return new Flashcard(id, english, "nghĩa", null, null, null, null, null);
    }
}
