package com.example.bivoca.ui.topics;
import com.example.bivoca.R;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class TopicPresentationTest {
    @Test public void unknownTopicsUseCobaltAndBook() {
        assertEquals(R.color.bivocal_light_cobalt, TopicPresentation.colorResourceFor(null));
        assertEquals(R.color.bivocal_light_cobalt, TopicPresentation.colorResourceFor("FUTURE_TOPIC"));
        assertEquals(R.drawable.topics16_book, TopicPresentation.iconFor("FUTURE_TOPIC"));
    }
    @Test public void preservesCatalogIconAndColorFamilies() {
        assertEquals(R.drawable.topics16_food, TopicPresentation.iconFor("FOOD"));
        assertEquals(R.drawable.topics16_people, TopicPresentation.iconFor("RELATIONSHIPS"));
        assertEquals(R.drawable.topics16_arts, TopicPresentation.iconFor("LEISURE"));
        assertEquals(R.color.bivocal_light_coral_ink, TopicPresentation.colorResourceFor("HEALTH"));
        assertEquals(R.color.bivocal_light_honey_ink, TopicPresentation.colorResourceFor("FINANCE"));
        assertEquals(R.color.bivocal_light_lavender_ink, TopicPresentation.colorResourceFor("LANGUAGE"));
        assertEquals(R.color.bivocal_light_mint_ink, TopicPresentation.colorResourceFor("SCIENCE"));
        assertEquals(R.color.bivocal_light_lavender_ink, TopicPresentation.colorResourceFor("CLOTHING"));
    }
    @Test public void retainsPartOfSpeechGlyphsAndSafeFallbacks() {
        assertEquals("", TopicPresentation.posGlyph(null));
        assertEquals("N", TopicPresentation.posGlyph("POS_NOUN"));
        assertEquals("V", TopicPresentation.posGlyph("POS_VERB"));
        assertEquals("Adj", TopicPresentation.posGlyph("POS_ADJECTIVE"));
        assertEquals("Adv", TopicPresentation.posGlyph("POS_ADVERB"));
        assertEquals("Prep", TopicPresentation.posGlyph("POS_PREPOSITION"));
        assertEquals("Conj", TopicPresentation.posGlyph("POS_CONJUNCTION"));
        assertEquals("Pron", TopicPresentation.posGlyph("POS_PRONOUN"));
        assertEquals("Aa", TopicPresentation.posGlyph("POS_OTHER"));
    }
}
