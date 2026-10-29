package com.example.bivoca.ui.topics;

import com.example.bivoca.data.model.Topic;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Local presentation filtering over the small API catalog, never the flashcard collection. */
public final class TopicFilter {
    public enum Group { ALL, SEMANTIC, PART_OF_SPEECH, LIFE, WORK, NATURE }
    private TopicFilter() {}
    public static List<Topic> apply(List<Topic> source, String query) {
        return apply(source,query,Group.ALL);
    }
    public static List<Topic> apply(List<Topic> source, String query, Group group) {
        if (source == null) return Collections.emptyList();
        String needle = normalize(query);
        List<Topic> result = new ArrayList<>();
        for (Topic topic : source) {
            if (topic != null && belongs(topic,group) && (needle.isEmpty()
                    || normalize(topic.getName()).contains(needle)
                    || normalize(topic.getCode()).contains(needle)
                    || normalize(topic.getDescription()).contains(needle))) result.add(topic);
        }
        // Stable partition: preserve API order inside each of the two sections.
        result.sort((a,b)->Boolean.compare(a.isPartOfSpeechGroup(),b.isPartOfSpeechGroup()));
        return result;
    }
    private static boolean belongs(Topic topic,Group group) {
        if(group==Group.ALL)return true;
        if(group==Group.PART_OF_SPEECH)return topic.isPartOfSpeechGroup();
        if(group==Group.SEMANTIC)return !topic.isPartOfSpeechGroup();
        if(topic.isPartOfSpeechGroup()||topic.getCode()==null)return false;
        String codes=group==Group.WORK?"|SCHOOL|WORK|TECHNOLOGY|FINANCE|COMMUNICATION|LAW_GOVERNMENT|LANGUAGE|"
                :group==Group.NATURE?"|ANIMALS|WEATHER|NATURE|SCIENCE|"
                :"|FOOD|FAMILY|TRAVEL|HEALTH|SHOPPING|PERSONAL|LEISURE|HOME|DAILY_LIFE|TRANSPORTATION|RELATIONSHIPS|CLOTHING|BUILDINGS_PLACES|SPORTS|EMOTIONS|TIME_CALENDAR|CULTURE|ARTS|";
        return codes.contains("|"+topic.getCode()+"|");
    }
    private static String normalize(String value) {
        if (value == null) return "";
        String text = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return text.replaceAll("\\p{M}+", "").replace('đ', 'd');
    }
}
