package com.example.bivoca.ui.topics;

import com.example.bivoca.data.model.Topic;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class TopicFilterTest {
    private static Topic topic(String name) throws Exception {
        Topic topic = new Topic();
        Field field = Topic.class.getDeclaredField("name");
        field.setAccessible(true);
        field.set(topic, name);
        return topic;
    }
    @Test public void filtersCaseAndAccentInsensitiveWithoutChangingSource() throws Exception {
        List<Topic> source = Arrays.asList(topic("Động vật"), topic("Food"));
        List<Topic> result = TopicFilter.apply(source, "dong");
        assertEquals(1, result.size());
        assertEquals("Động vật", result.get(0).getName());
        assertEquals(2, source.size());
    }
    private static Topic row(int id,String code,String name,String description,boolean pos) throws Exception {
        Topic result=new Topic(id,code,name,description,null,100);
        Field field=Topic.class.getDeclaredField("topicGroup");field.setAccessible(true);
        field.set(result,pos?"PART_OF_SPEECH":"SEMANTIC");return result;
    }
    @Test public void searchesVietnameseDescriptionAndCodeAndCombinesGroupFilter() throws Exception {
        List<Topic> source=Arrays.asList(row(1,"FOOD","Food","Thức ăn và đồ uống",false),row(2,"POS_NOUN","Noun","Danh từ",true));
        assertEquals(1,TopicFilter.apply(source,"thuc an",TopicFilter.Group.ALL).size());
        assertEquals(1,TopicFilter.apply(source,"pos_noun",TopicFilter.Group.ALL).size());
        assertTrue(TopicFilter.apply(source,"Food",TopicFilter.Group.PART_OF_SPEECH).isEmpty());
        assertEquals("Noun",TopicFilter.apply(source,"",TopicFilter.Group.PART_OF_SPEECH).get(0).getName());
    }
    @Test public void stableSemanticFirstAndExactCategoryCodesDoNotMutateCatalog() throws Exception {
        Topic noun=row(1,"POS_NOUN","Noun",null,true);
        Topic food=row(2,"FOOD","Food",null,false);
        Topic misleading=row(3,"SEAFOOD","Seafood",null,false);
        List<Topic> source=Arrays.asList(noun,food,misleading);
        assertEquals(food,TopicFilter.apply(source,"",TopicFilter.Group.ALL).get(0));
        assertEquals(noun,source.get(0));
        assertEquals(Arrays.asList(food),TopicFilter.apply(source,"",TopicFilter.Group.LIFE));
        assertTrue(TopicFilter.apply(null,"",TopicFilter.Group.ALL).isEmpty());
    }
}
