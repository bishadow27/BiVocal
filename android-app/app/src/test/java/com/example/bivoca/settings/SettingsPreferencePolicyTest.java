package com.example.bivoca.settings;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SettingsPreferencePolicyTest {
    @Test public void dailyGoalAcceptsOnlySupportedValues() {
        assertEquals(5, SettingsPreferenceManager.normalizeDailyGoal(5));
        assertEquals(10, SettingsPreferenceManager.normalizeDailyGoal(10));
        assertEquals(15, SettingsPreferenceManager.normalizeDailyGoal(15));
        assertEquals(20, SettingsPreferenceManager.normalizeDailyGoal(20));
        assertEquals(10, SettingsPreferenceManager.normalizeDailyGoal(12));
    }

    @Test public void flashcardFrontDefaultsSafelyAndSupportsVietnamese() {
        assertEquals(SettingsPreferenceManager.FRONT_ENGLISH,
                SettingsPreferenceManager.normalizeFront(null));
        assertEquals(SettingsPreferenceManager.FRONT_VIETNAMESE,
                SettingsPreferenceManager.normalizeFront(SettingsPreferenceManager.FRONT_VIETNAMESE));
    }

    @Test public void speechRateAcceptsOnlyThreeSupportedRates() {
        assertEquals(0.8f, SettingsPreferenceManager.normalizeSpeechRate(0.8f), 0.001f);
        assertEquals(1.0f, SettingsPreferenceManager.normalizeSpeechRate(0.95f), 0.001f);
        assertEquals(1.2f, SettingsPreferenceManager.normalizeSpeechRate(1.2f), 0.001f);
    }

    @Test public void reminderDaysRoundTripWithoutInvalidBits() {
        int mask = 0;
        mask = SettingsPreferenceManager.setDaySelected(mask, Calendar.MONDAY, true);
        mask = SettingsPreferenceManager.setDaySelected(mask, Calendar.FRIDAY, true);
        assertTrue(SettingsPreferenceManager.isDaySelected(mask, Calendar.MONDAY));
        assertTrue(SettingsPreferenceManager.isDaySelected(mask, Calendar.FRIDAY));
        assertFalse(SettingsPreferenceManager.isDaySelected(mask, Calendar.SUNDAY));
        assertEquals(SettingsPreferenceManager.ALL_DAYS,
                SettingsPreferenceManager.normalizeDays(0xFFFF));
    }
}
