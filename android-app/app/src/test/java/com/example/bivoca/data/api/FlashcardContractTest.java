package com.example.bivoca.data.api;

import com.example.bivoca.data.model.Flashcard;
import com.google.gson.Gson;

import org.junit.Test;

import okhttp3.Request;

import static org.junit.Assert.*;

public class FlashcardContractTest {
    @Test public void publicRequestUsesTopicPathWithoutBearerOrQuery() {
        Request request = RetrofitClient.getInstance().create(FlashcardApi.class)
                .getFlashcards("TRAVEL").request();
        assertEquals("http://127.0.0.1:3000/api/v1/topics/TRAVEL/flashcards", request.url().toString());
        assertEquals("GET", request.method());
        assertNull(request.url().query());
        assertNull(request.header("Authorization"));
    }

    @Test public void mapsAllPublicFieldsAndNullableMetadata() {
        Flashcard card = new Gson().fromJson("{\"id\":1,\"english\":\"apple\",\"vietnamese\":\"quả táo\","
                + "\"pronunciation\":\"/ˈæpəl/\",\"partOfSpeech\":\"noun\",\"example\":\"I eat an apple every morning.\","
                + "\"exampleVi\":\"Tôi ăn một quả táo mỗi sáng.\",\"level\":\"A1\","
                + "\"contentStatus\":\"IMPORTED_UNVERIFIED\"}", Flashcard.class);
        assertEquals(1, card.getId());
        assertEquals("apple", card.getEnglish());
        assertEquals("quả táo", card.getVietnamese());
        assertEquals("/ˈæpəl/", card.getPronunciation());
        assertEquals("noun", card.getPartOfSpeech());
        assertEquals("I eat an apple every morning.", card.getExample());
        assertEquals("Tôi ăn một quả táo mỗi sáng.", card.getExampleVi());
        assertEquals("A1", card.getLevel());
        assertEquals("IMPORTED_UNVERIFIED", card.getContentStatus());
        assertTrue(card.isImportedUnverified());

        Flashcard nullable = new Gson().fromJson("{\"id\":1,\"english\":\"apple\",\"vietnamese\":\"quả táo\"}", Flashcard.class);
        assertNull(nullable.getPronunciation());
        assertNull(nullable.getPartOfSpeech());
        assertNull(nullable.getExample());
        assertNull(nullable.getExampleVi());
        assertNull(nullable.getLevel());
    }
}
