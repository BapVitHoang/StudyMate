package com.hcmute.studymate.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        String noteId = intent.getStringExtra(NotificationHelper.EXTRA_NOTE_ID);
        String title = intent.getStringExtra(NotificationHelper.EXTRA_NOTE_TITLE);
        String content = intent.getStringExtra(NotificationHelper.EXTRA_NOTE_CONTENT);
        NotificationHelper.showReminderNotification(context, noteId, title, content);
    }
}
