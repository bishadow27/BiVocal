package com.example.bivoca.settings;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.bivoca.R;
import com.example.bivoca.ui.home.HomeActivity;
import com.example.bivoca.utils.SessionManager;

public final class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "study_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                || Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())
                || Intent.ACTION_TIME_CHANGED.equals(intent.getAction())) {
            ReminderScheduler.reconcile(context);
            return;
        }

        int expectedUserId = intent.getIntExtra(ReminderScheduler.EXTRA_USER_ID, -1);
        int day = intent.getIntExtra(ReminderScheduler.EXTRA_DAY, -1);
        SessionManager session = new SessionManager(context);
        SettingsPreferenceManager settings = new SettingsPreferenceManager(context);
        if (!session.isLoggedIn() || session.getUserId() != expectedUserId
                || settings.getUserId() != expectedUserId
                || !settings.isReminderEnabled()
                || !SettingsPreferenceManager.isDaySelected(settings.getReminderDays(), day)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ReminderScheduler.reconcile(context);
            return;
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                    context.getString(R.string.settings_reminder_channel),
                    NotificationManager.IMPORTANCE_DEFAULT));
        }
        Intent open = new Intent(context, HomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content = PendingIntent.getActivity(context, 4100, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.notify(4100, new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_bivocal)
                .setContentTitle(context.getString(R.string.settings_reminder_notification_title))
                .setContentText(context.getString(R.string.settings_reminder_notification_body))
                .setAutoCancel(true)
                .setContentIntent(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build());
        ReminderScheduler.reconcile(context);
    }
}
