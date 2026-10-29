package com.example.bivoca.settings;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.service.notification.StatusBarNotification;

import androidx.core.content.ContextCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/** Exercises the real receiver with an isolated temporary session and restores installed app data. */
@RunWith(AndroidJUnit4.class)
public final class ReminderReceiverTest {
    private static final int TEST_USER_ID = 876543;
    private Context context;
    private SharedPreferences session;
    private Map<String, ?> savedSession;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assumeTrue(ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED);
        session = context.getSharedPreferences("bivoca_session", Context.MODE_PRIVATE);
        savedSession = new HashMap<>(session.getAll());
        session.edit().clear()
                .putString("access_token", "instrumentation-only-token")
                .putInt("user_id", TEST_USER_ID)
                .putString("username", "instrumentation")
                .commit();
        SettingsPreferenceManager settings = new SettingsPreferenceManager(context);
        settings.setReminderEnabled(true);
        settings.setReminderDays(SettingsPreferenceManager.ALL_DAYS);
    }

    @After
    public void tearDown() {
        ReminderScheduler.cancelForUser(context, TEST_USER_ID);
        context.getSharedPreferences("bivoca_user_settings_" + TEST_USER_ID, Context.MODE_PRIVATE)
                .edit().clear().commit();
        SharedPreferences.Editor restore = session.edit().clear();
        for (Map.Entry<String, ?> entry : savedSession.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) restore.putString(entry.getKey(), (String) value);
            else if (value instanceof Integer) restore.putInt(entry.getKey(), (Integer) value);
            else if (value instanceof Long) restore.putLong(entry.getKey(), (Long) value);
            else if (value instanceof Float) restore.putFloat(entry.getKey(), (Float) value);
            else if (value instanceof Boolean) restore.putBoolean(entry.getKey(), (Boolean) value);
        }
        restore.commit();
        ReminderScheduler.reconcile(context);
    }

    @Test
    public void configuredReminderPostsTheAppNotification() {
        int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        Intent reminder = new Intent(context, ReminderReceiver.class)
                .setAction("com.example.bivoca.ACTION_STUDY_REMINDER")
                .putExtra(ReminderScheduler.EXTRA_USER_ID, TEST_USER_ID)
                .putExtra(ReminderScheduler.EXTRA_DAY, today);

        new ReminderReceiver().onReceive(context, reminder);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        boolean found = false;
        for (StatusBarNotification notification : manager.getActiveNotifications()) {
            if (notification.getId() == 4100) {
                found = true;
                break;
            }
        }
        assertTrue("BiVocal study reminder should be active", found);
    }
}
