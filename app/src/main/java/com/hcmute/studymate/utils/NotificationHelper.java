package com.hcmute.studymate.utils;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.hcmute.studymate.R;
import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.model.Reminder;
import com.hcmute.studymate.view.NoteDetailActivity;
import com.hcmute.studymate.view.NoteListActivity;

public final class NotificationHelper {
    public static final String EXTRA_NOTE_ID = "extra_note_id";
    public static final String EXTRA_NOTE_TITLE = "extra_note_title";
    public static final String EXTRA_NOTE_CONTENT = "extra_note_content";

    private static final String CHANNEL_ID = "study_reminders";
    private static final String CHANNEL_NAME = "Study reminders";
    private static final String REVIEW_CHANNEL_ID = "daily_review";
    private static final String REVIEW_CHANNEL_NAME = "Daily review";
    private static final String REMINDER_ACTION = "com.hcmute.studymate.REMINDER";

    private NotificationHelper() {
    }

    public static void scheduleReminder(Context context, Reminder reminder, Note note) {
        if (context == null || reminder == null || note == null || reminder.getReminderAt() <= 0L) {
            return;
        }

        Context appContext = context.getApplicationContext();
        Intent intent = new Intent(appContext, ReminderReceiver.class);
        intent.setAction(REMINDER_ACTION);
        intent.putExtra(EXTRA_NOTE_ID, note.getId());
        intent.putExtra(EXTRA_NOTE_TITLE, note.getTitle());
        intent.putExtra(EXTRA_NOTE_CONTENT, note.getContent());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                appContext,
                buildRequestCode(note.getId()),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminder.getReminderAt(), pendingIntent);
        }
    }

    public static void showReminderNotification(Context context, String noteId, String title, String content) {
        if (context == null || !canPostNotifications(context)) {
            return;
        }

        Context appContext = context.getApplicationContext();
        createReminderChannel(appContext);

        Intent detailIntent = NoteDetailActivity.newIntent(appContext, noteId);
        detailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                appContext,
                buildRequestCode(noteId),
                detailIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String notificationTitle = isBlank(title) ? "StudyMate reminder" : title;
        String notificationContent = isBlank(content) ? "Open your study note." : content;
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(appContext, CHANNEL_ID)
                : new Notification.Builder(appContext);

        Notification notification = builder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setStyle(new Notification.BigTextStyle().bigText(notificationContent))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build();

        NotificationManager notificationManager =
                (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(buildRequestCode(noteId), notification);
        }
    }

    public static void showDailyReviewNotification(Context context, int reviewCount, String firstTitle) {
        if (context == null || reviewCount <= 0 || !canPostNotifications(context)) {
            return;
        }

        Context appContext = context.getApplicationContext();
        createDailyReviewChannel(appContext);

        Intent listIntent = new Intent(appContext, NoteListActivity.class);
        listIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                appContext,
                2407,
                listIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "Daily StudyMate review";
        String content = reviewCount == 1
                ? "One note is ready to review: " + safeTitle(firstTitle)
                : reviewCount + " notes are ready to review.";

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(appContext, REVIEW_CHANNEL_ID)
                : new Notification.Builder(appContext);

        Notification notification = builder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new Notification.BigTextStyle().bigText(content))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build();

        NotificationManager notificationManager =
                (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(2407, notification);
        }
    }

    public static boolean canPostNotifications(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private static void createReminderChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null || notificationManager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Notifications for StudyMate note reminders");
        notificationManager.createNotificationChannel(channel);
    }

    private static void createDailyReviewChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null || notificationManager.getNotificationChannel(REVIEW_CHANNEL_ID) != null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                REVIEW_CHANNEL_ID,
                REVIEW_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Daily reminders to review older StudyMate notes");
        notificationManager.createNotificationChannel(channel);
    }

    private static int buildRequestCode(String noteId) {
        return noteId == null ? 0 : noteId.hashCode();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safeTitle(String title) {
        return isBlank(title) ? "your study note" : title;
    }
}
