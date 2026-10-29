package com.example.bivoca.settings;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.Calendar;

/** Owns only BiVocal's seven inexact weekly reminder alarms. */
public final class ReminderScheduler {
    static final String EXTRA_USER_ID = "reminder_user_id";
    static final String EXTRA_DAY = "reminder_day";
    private static final long WINDOW_MS = 15L * 60L * 1000L;

    private ReminderScheduler() { }

    public static void reconcile(Context context) {
        SettingsPreferenceManager settings = new SettingsPreferenceManager(context);
        cancelForUser(context, settings.getUserId());
        if (!settings.isReminderEnabled() || settings.getReminderDays() == 0) return;
        for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
            if (SettingsPreferenceManager.isDaySelected(settings.getReminderDays(), day)) {
                scheduleDay(context, settings, day, System.currentTimeMillis());
            }
        }
    }

    static void scheduleDay(Context context, SettingsPreferenceManager settings,
                            int calendarDay, long nowMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        long triggerAt = nextTriggerMillis(nowMillis, calendarDay,
                settings.getReminderHour(), settings.getReminderMinute());
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, WINDOW_MS,
                reminderIntent(context, settings.getUserId(), calendarDay,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    }

    public static void cancelForUser(Context context, int userId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null || userId < 0) return;
        for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
            PendingIntent pending = reminderIntent(context, userId, day,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pending != null) {
                alarmManager.cancel(pending);
                pending.cancel();
            }
        }
    }

    static long nextTriggerMillis(long nowMillis, int calendarDay, int hour, int minute) {
        Calendar next = Calendar.getInstance();
        next.setTimeInMillis(nowMillis);
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        int delta = (calendarDay - next.get(Calendar.DAY_OF_WEEK) + 7) % 7;
        if (delta == 0 && next.getTimeInMillis() <= nowMillis) delta = 7;
        next.add(Calendar.DAY_OF_YEAR, delta);
        return next.getTimeInMillis();
    }

    private static PendingIntent reminderIntent(Context context, int userId, int day, int flags) {
        Intent intent = new Intent(context, ReminderReceiver.class)
                .setAction("com.example.bivoca.ACTION_STUDY_REMINDER")
                .setData(Uri.parse("bivoca://reminder/" + userId + "/" + day))
                .putExtra(EXTRA_USER_ID, userId)
                .putExtra(EXTRA_DAY, day);
        return PendingIntent.getBroadcast(context, requestCode(userId, day), intent, flags);
    }

    static int requestCode(int userId, int day) {
        return 12000 + (Math.abs(userId) % 10000) * 10 + day;
    }
}
