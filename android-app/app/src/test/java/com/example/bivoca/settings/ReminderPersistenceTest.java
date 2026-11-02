package com.example.bivoca.settings;

import android.content.SharedPreferences;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class ReminderPersistenceTest {
    @Test public void enabledScheduleRequiresAtLeastOneDayAndValidTime() {
        assertFalse(new ReminderSchedule(true,20,0,0).isValid());
        assertTrue(new ReminderSchedule(false,20,0,0).isValid());
        assertFalse(new ReminderSchedule(true,24,0,1).isValid());
        assertFalse(new ReminderSchedule(true,20,60,1).isValid());
        assertFalse(new ReminderSchedule(true,20,0,128).isValid());
        assertFalse(new ReminderSchedule(true,20,-1,1).isValid());
    }
    @Test public void editsProduceAnIndependentDraft() {
        ReminderSchedule original=new ReminderSchedule(false,20,0,127);
        ReminderSchedule draft=original.withEnabled(true).withTime(7,35).withDays(42);
        assertEquals(new ReminderSchedule(false,20,0,127),original);
        assertEquals(new ReminderSchedule(true,7,35,42),draft);
        assertNotEquals(original,draft);
    }
    @Test public void oneCommitPersistsAllFourKeysAndKeepsUnrelatedPreferences() {
        MemoryPreferences memory=new MemoryPreferences();memory.values.put("daily_goal",15);
        SettingsPreferenceManager manager=new SettingsPreferenceManager(memory,new MemoryPreferences(),17);
        ReminderSchedule target=new ReminderSchedule(true,7,35,42);
        assertTrue(manager.saveReminderSchedule(target));
        assertEquals(1,memory.commits);assertEquals(target,manager.readReminderSchedule());
        assertEquals(15,memory.values.get("daily_goal"));assertEquals(memory.values,memory.disk);
    }
    @Test public void failedCommitRestoresAbsentKeysAndLeavesDiskAndUnrelatedValuesIntact() {
        MemoryPreferences memory=new MemoryPreferences();memory.values.put("daily_goal",15);memory.disk.putAll(memory.values);
        Map<String,Object> before=new HashMap<>(memory.values);memory.failNext=true;
        SettingsPreferenceManager manager=new SettingsPreferenceManager(memory,new MemoryPreferences(),17);
        assertFalse(manager.saveReminderSchedule(new ReminderSchedule(true,7,35,42)));
        assertEquals(before,memory.values);assertEquals(before,memory.disk);
        assertEquals(new ReminderSchedule(false,20,0,127),manager.readReminderSchedule());
    }
    @Test public void failedCommitRestoresExistingFourFieldsNotTheDraft() {
        MemoryPreferences memory=new MemoryPreferences();
        SettingsPreferenceManager manager=new SettingsPreferenceManager(memory,new MemoryPreferences(),17);
        ReminderSchedule original=new ReminderSchedule(false,9,5,3);
        assertTrue(manager.saveReminderSchedule(original));memory.failNext=true;
        assertFalse(manager.saveReminderSchedule(new ReminderSchedule(true,7,35,42)));
        assertEquals(original,manager.readReminderSchedule());assertEquals(memory.values,memory.disk);
    }
    @Test public void invalidDraftOrMissingAccountDoesNotEditPreferences() {
        MemoryPreferences memory=new MemoryPreferences();
        assertFalse(new SettingsPreferenceManager(memory,memory,17).saveReminderSchedule(new ReminderSchedule(true,20,0,0)));
        assertFalse(new SettingsPreferenceManager(memory,memory,0).saveReminderSchedule(new ReminderSchedule(true,20,0,1)));
        assertEquals(0,memory.commits);assertTrue(memory.values.isEmpty());
    }

    /** Models Android's memory-before-disk behavior, including a failed disk commit. */
    private static final class MemoryPreferences implements SharedPreferences {
        final Map<String,Object> values=new HashMap<>(),disk=new HashMap<>();boolean failNext;int commits;
        public Map<String,?> getAll(){return new HashMap<>(values);}
        public String getString(String k,String d){return (String)values.getOrDefault(k,d);}
        @SuppressWarnings("unchecked") public Set<String> getStringSet(String k,Set<String>d){return (Set<String>)values.getOrDefault(k,d);}
        public int getInt(String k,int d){return (Integer)values.getOrDefault(k,d);}
        public long getLong(String k,long d){return (Long)values.getOrDefault(k,d);}
        public float getFloat(String k,float d){return (Float)values.getOrDefault(k,d);}
        public boolean getBoolean(String k,boolean d){return (Boolean)values.getOrDefault(k,d);}
        public boolean contains(String k){return values.containsKey(k);}
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
        public Editor edit(){return new Editor(){
            final Map<String,Object> updates=new HashMap<>();boolean clear;
            public Editor putString(String k,String v){updates.put(k,v);return this;}
            public Editor putStringSet(String k,Set<String>v){updates.put(k,v);return this;}
            public Editor putInt(String k,int v){updates.put(k,v);return this;}
            public Editor putLong(String k,long v){updates.put(k,v);return this;}
            public Editor putFloat(String k,float v){updates.put(k,v);return this;}
            public Editor putBoolean(String k,boolean v){updates.put(k,v);return this;}
            public Editor remove(String k){updates.put(k,null);return this;}
            public Editor clear(){clear=true;return this;}
            public boolean commit(){commits++;if(clear)values.clear();updates.forEach((k,v)->{if(v==null)values.remove(k);else values.put(k,v);});
                if(failNext){failNext=false;return false;}disk.clear();disk.putAll(values);return true;}
            public void apply(){commit();}
        };}
    }
}
