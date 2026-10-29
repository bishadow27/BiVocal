package com.example.bivoca.settings;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReminderSchedulerTest {
    @Test public void nextTriggerUsesDeviceCalendarAndMovesPastElapsedTime() {
        TimeZone original = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        try {
            Calendar now = Calendar.getInstance();
            now.set(2026, Calendar.OCTOBER, 19, 21, 0, 0);
            now.set(Calendar.MILLISECOND, 0); // Monday after the requested 20:00.
            long trigger = ReminderScheduler.nextTriggerMillis(now.getTimeInMillis(),
                    Calendar.MONDAY, 20, 0);
            Calendar result = Calendar.getInstance();
            result.setTimeInMillis(trigger);
            assertEquals(Calendar.MONDAY, result.get(Calendar.DAY_OF_WEEK));
            assertEquals(20, result.get(Calendar.HOUR_OF_DAY));
            assertTrue(trigger - now.getTimeInMillis() > 6L * 24L * 60L * 60L * 1000L);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test public void eachUserAndDayHasStableDistinctRequestCode() {
        assertEquals(ReminderScheduler.requestCode(55, Calendar.MONDAY),
                ReminderScheduler.requestCode(55, Calendar.MONDAY));
        assertTrue(ReminderScheduler.requestCode(55, Calendar.MONDAY)
                != ReminderScheduler.requestCode(55, Calendar.TUESDAY));
        assertTrue(ReminderScheduler.requestCode(55, Calendar.MONDAY)
                != ReminderScheduler.requestCode(56, Calendar.MONDAY));
    }
}
