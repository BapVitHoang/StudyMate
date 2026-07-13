package com.hcmute.studymate.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.hcmute.studymate.R;
import com.hcmute.studymate.view.NoteDetailActivity;

public class FocusTimerService extends Service {
    public static final String ACTION_START = "com.hcmute.studymate.focus.START";
    public static final String ACTION_PAUSE = "com.hcmute.studymate.focus.PAUSE";
    public static final String ACTION_RESUME = "com.hcmute.studymate.focus.RESUME";
    public static final String ACTION_STOP = "com.hcmute.studymate.focus.STOP";
    public static final String ACTION_TICK = "com.hcmute.studymate.focus.TICK";
    public static final String ACTION_FINISHED = "com.hcmute.studymate.focus.FINISHED";

    public static final String EXTRA_NOTE_ID = "extra_note_id";
    public static final String EXTRA_NOTE_TITLE = "extra_note_title";
    public static final String EXTRA_DURATION_MILLIS = "extra_duration_millis";
    public static final String EXTRA_REMAINING_MILLIS = "extra_remaining_millis";
    public static final String EXTRA_RUNNING = "extra_running";

    private static final String CHANNEL_ID = "focus_timer";
    private static final String CHANNEL_NAME = "Focus timer";
    private static final int NOTIFICATION_ID = 4201;
    private static final long TICK_MILLIS = 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final FocusBinder binder = new FocusBinder();

    private String noteId;
    private String noteTitle;
    private long durationMillis;
    private long remainingMillis;
    private boolean running;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            remainingMillis = Math.max(0L, remainingMillis - TICK_MILLIS);
            sendTick(ACTION_TICK);
            updateNotification();
            if (remainingMillis <= 0L) {
                finishSession();
                return;
            }
            handler.postDelayed(this, TICK_MILLIS);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            startSession(
                    intent.getStringExtra(EXTRA_NOTE_ID),
                    intent.getStringExtra(EXTRA_NOTE_TITLE),
                    intent.getLongExtra(EXTRA_DURATION_MILLIS, 25L * 60L * 1000L)
            );
        } else if (ACTION_PAUSE.equals(action)) {
            pauseSession();
        } else if (ACTION_RESUME.equals(action)) {
            resumeSession();
        } else if (ACTION_STOP.equals(action)) {
            stopSession();
        }
        return START_NOT_STICKY;
    }

    public void startSession(String noteId, String noteTitle, long durationMillis) {
        this.noteId = noteId;
        this.noteTitle = isBlank(noteTitle) ? "Study session" : noteTitle;
        this.durationMillis = durationMillis;
        this.remainingMillis = durationMillis;
        this.running = true;
        createChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        sendTick(ACTION_TICK);
        handler.removeCallbacks(tickRunnable);
        handler.postDelayed(tickRunnable, TICK_MILLIS);
    }

    public void pauseSession() {
        if (remainingMillis <= 0L) {
            return;
        }
        running = false;
        handler.removeCallbacks(tickRunnable);
        sendTick(ACTION_TICK);
        updateNotification();
    }

    public void resumeSession() {
        if (remainingMillis <= 0L) {
            return;
        }
        running = true;
        sendTick(ACTION_TICK);
        updateNotification();
        handler.removeCallbacks(tickRunnable);
        handler.postDelayed(tickRunnable, TICK_MILLIS);
    }

    public void stopSession() {
        running = false;
        handler.removeCallbacks(tickRunnable);
        remainingMillis = 0L;
        sendTick(ACTION_TICK);
        stopForeground(true);
        stopSelf();
    }

    public boolean hasActiveSession() {
        return noteId != null && durationMillis > 0L && remainingMillis > 0L;
    }

    public String getNoteId() {
        return noteId;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public long getRemainingMillis() {
        return remainingMillis;
    }

    public boolean isRunning() {
        return running;
    }

    private void finishSession() {
        running = false;
        handler.removeCallbacks(tickRunnable);
        sendTick(ACTION_FINISHED);
        stopForeground(true);
        stopSelf();
    }

    private Notification buildNotification() {
        Intent detailIntent = NoteDetailActivity.newIntent(this, noteId);
        detailIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                91,
                detailIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent toggleIntent = new Intent(this, FocusTimerService.class);
        toggleIntent.setAction(running ? ACTION_PAUSE : ACTION_RESUME);
        PendingIntent togglePendingIntent = PendingIntent.getService(
                this,
                92,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, FocusTimerService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                93,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        return builder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Focus: " + noteTitle)
                .setContentText(formatRemaining(remainingMillis))
                .setContentIntent(contentIntent)
                .setOngoing(running)
                .addAction(0, running ? "Pause" : "Resume", togglePendingIntent)
                .addAction(0, "Stop", stopPendingIntent)
                .build();
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null && hasActiveSession()) {
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private void sendTick(String action) {
        Intent intent = new Intent(action);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_NOTE_ID, noteId);
        intent.putExtra(EXTRA_DURATION_MILLIS, durationMillis);
        intent.putExtra(EXTRA_REMAINING_MILLIS, remainingMillis);
        intent.putExtra(EXTRA_RUNNING, running);
        sendBroadcast(intent);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Keeps the StudyMate focus timer visible while studying");
        manager.createNotificationChannel(channel);
    }

    private String formatRemaining(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(java.util.Locale.US, "%02d:%02d remaining", minutes, seconds);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public class FocusBinder extends Binder {
        public FocusTimerService getService() {
            return FocusTimerService.this;
        }
    }
}
