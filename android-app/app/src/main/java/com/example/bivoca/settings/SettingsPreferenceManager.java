package com.example.bivoca.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.bivoca.utils.SessionManager;

import java.util.Locale;

/** Stores device display preferences separately from account-scoped learning preferences. */
public final class SettingsPreferenceManager {
    public static final int DEFAULT_DAILY_GOAL = 10;
    public static final String FRONT_ENGLISH = "EN";
    public static final String FRONT_VIETNAMESE = "VI";
    public static final String TEXT_STANDARD = "STANDARD";
    public static final String TEXT_LARGE = "LARGE";
    public static final int DEFAULT_REMINDER_HOUR = 20;
    public static final int DEFAULT_REMINDER_MINUTE = 0;
    public static final int ALL_DAYS = 0x7F;

    private static final String DEVICE_PREFS = "bivoca_device_settings";
    private static final String USER_PREFS_PREFIX = "bivoca_user_settings_";
    private static final String KEY_DAILY_GOAL = "daily_goal";
    private static final String KEY_FLASHCARD_FRONT = "flashcard_front";
    private static final String KEY_AUTO_PRONUNCIATION = "auto_pronunciation";
    private static final String KEY_SPEECH_RATE = "speech_rate";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";
    private static final String KEY_REMINDER_DAYS = "reminder_days";
    private static final String KEY_TEXT_SIZE = "text_size";
    private static final String KEY_REDUCE_MOTION = "reduce_motion";

    private final SharedPreferences userPreferences;
    private final SharedPreferences devicePreferences;
    private final int userId;

    public SettingsPreferenceManager(Context context) {
        Context app = context.getApplicationContext();
        userId = new SessionManager(app).getUserId();
        userPreferences = app.getSharedPreferences(USER_PREFS_PREFIX + userId, Context.MODE_PRIVATE);
        devicePreferences = app.getSharedPreferences(DEVICE_PREFS, Context.MODE_PRIVATE);
    }

    SettingsPreferenceManager(SharedPreferences user, SharedPreferences device, int owner) {
        userPreferences=user; devicePreferences=device; userId=owner;
    }

    public ReminderSchedule readReminderSchedule() {
        return new ReminderSchedule(isReminderEnabled(),getReminderHour(),getReminderMinute(),getReminderDays());
    }

    /** Worker-thread only: confirm all four fields together before scheduling. */
    public synchronized boolean saveReminderSchedule(ReminderSchedule value) {
        if (userId <= 0 || value == null || !value.isValid()) return false;
        String[] keys={KEY_REMINDER_ENABLED,KEY_REMINDER_HOUR,KEY_REMINDER_MINUTE,KEY_REMINDER_DAYS};
        java.util.Map<String,?> old=userPreferences.getAll();
        try {
            if (userPreferences.edit().putBoolean(KEY_REMINDER_ENABLED,value.enabled)
                    .putInt(KEY_REMINDER_HOUR,value.hour).putInt(KEY_REMINDER_MINUTE,value.minute)
                    .putInt(KEY_REMINDER_DAYS,value.days).commit()) return true;
        } catch (RuntimeException ignored) { /* Never expose a false success. */ }
        // A failed disk commit may already have updated SharedPreferences' memory map.
        // Restore only these four keys, preserving absence and unrelated account settings.
        SharedPreferences.Editor rollback=userPreferences.edit();
        for (String key:keys) {
            Object previous=old.get(key);
            if (!old.containsKey(key)) rollback.remove(key);
            else if (previous instanceof Boolean) rollback.putBoolean(key,(Boolean)previous);
            else if (previous instanceof Integer) rollback.putInt(key,(Integer)previous);
        }
        try { rollback.commit(); } catch (RuntimeException ignored) { }
        return false;
    }

    public int getUserId() { return userId; }

    public int getDailyGoal() {
        return normalizeDailyGoal(userPreferences.getInt(KEY_DAILY_GOAL, DEFAULT_DAILY_GOAL));
    }

    public void setDailyGoal(int value) {
        userPreferences.edit().putInt(KEY_DAILY_GOAL, normalizeDailyGoal(value)).apply();
    }

    public String getFlashcardFront() {
        return normalizeFront(userPreferences.getString(KEY_FLASHCARD_FRONT, FRONT_ENGLISH));
    }

    public void setFlashcardFront(String value) {
        userPreferences.edit().putString(KEY_FLASHCARD_FRONT, normalizeFront(value)).apply();
    }

    public boolean isAutoPronunciationEnabled() {
        return userPreferences.getBoolean(KEY_AUTO_PRONUNCIATION, false);
    }

    public void setAutoPronunciationEnabled(boolean value) {
        userPreferences.edit().putBoolean(KEY_AUTO_PRONUNCIATION, value).apply();
    }

    public float getSpeechRate() {
        return normalizeSpeechRate(userPreferences.getFloat(KEY_SPEECH_RATE, 1.0f));
    }

    public void setSpeechRate(float value) {
        userPreferences.edit().putFloat(KEY_SPEECH_RATE, normalizeSpeechRate(value)).apply();
    }

    public boolean isReminderEnabled() {
        return userPreferences.getBoolean(KEY_REMINDER_ENABLED, false);
    }

    public void setReminderEnabled(boolean value) {
        userPreferences.edit().putBoolean(KEY_REMINDER_ENABLED, value).apply();
    }

    public int getReminderHour() {
        return clamp(userPreferences.getInt(KEY_REMINDER_HOUR, DEFAULT_REMINDER_HOUR), 0, 23);
    }

    public int getReminderMinute() {
        return clamp(userPreferences.getInt(KEY_REMINDER_MINUTE, DEFAULT_REMINDER_MINUTE), 0, 59);
    }

    public void setReminderTime(int hour, int minute) {
        userPreferences.edit()
                .putInt(KEY_REMINDER_HOUR, clamp(hour, 0, 23))
                .putInt(KEY_REMINDER_MINUTE, clamp(minute, 0, 59))
                .apply();
    }

    public int getReminderDays() {
        return normalizeDays(userPreferences.getInt(KEY_REMINDER_DAYS, ALL_DAYS));
    }

    public void setReminderDays(int days) {
        userPreferences.edit().putInt(KEY_REMINDER_DAYS, normalizeDays(days)).apply();
    }

    public String getTextSize() {
        String value = devicePreferences.getString(KEY_TEXT_SIZE, TEXT_STANDARD);
        return TEXT_LARGE.equals(value) ? TEXT_LARGE : TEXT_STANDARD;
    }

    public void setTextSize(String value) {
        devicePreferences.edit().putString(KEY_TEXT_SIZE,
                TEXT_LARGE.equals(value) ? TEXT_LARGE : TEXT_STANDARD).apply();
    }

    public boolean isReduceMotionEnabled() {
        return devicePreferences.getBoolean(KEY_REDUCE_MOTION, false);
    }

    public void setReduceMotionEnabled(boolean value) {
        devicePreferences.edit().putBoolean(KEY_REDUCE_MOTION, value).apply();
    }

    public float scaleText(float standardSp) {
        return TEXT_LARGE.equals(getTextSize()) ? standardSp * 1.16f : standardSp;
    }

    public String reminderTimeLabel() {
        return String.format(Locale.getDefault(), "%02d:%02d", getReminderHour(), getReminderMinute());
    }

    public static int normalizeDailyGoal(int value) {
        return value == 5 || value == 10 || value == 15 || value == 20 ? value : DEFAULT_DAILY_GOAL;
    }

    public static String normalizeFront(String value) {
        return FRONT_VIETNAMESE.equals(value) ? FRONT_VIETNAMESE : FRONT_ENGLISH;
    }

    public static float normalizeSpeechRate(float value) {
        if (Math.abs(value - 0.8f) < 0.05f) return 0.8f;
        if (Math.abs(value - 1.2f) < 0.05f) return 1.2f;
        return 1.0f;
    }

    public static int normalizeDays(int value) {
        return value & ALL_DAYS;
    }

    public static boolean isDaySelected(int mask, int calendarDay) {
        if (calendarDay < 1 || calendarDay > 7) return false;
        return (normalizeDays(mask) & (1 << (calendarDay - 1))) != 0;
    }

    public static int setDaySelected(int mask, int calendarDay, boolean selected) {
        if (calendarDay < 1 || calendarDay > 7) return normalizeDays(mask);
        int bit = 1 << (calendarDay - 1);
        return normalizeDays(selected ? mask | bit : mask & ~bit);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
