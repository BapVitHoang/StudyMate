package com.hcmute.studymate.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.hcmute.studymate.R;
import com.hcmute.studymate.view.NoteDetailActivity;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class VoiceRecordingService extends Service {
    public static final String ACTION_TICK = "com.hcmute.studymate.voice.TICK";
    public static final String ACTION_STOP = "com.hcmute.studymate.voice.STOP";
    public static final String EXTRA_NOTE_ID = "extra_note_id";
    public static final String EXTRA_NOTE_TITLE = "extra_note_title";
    public static final String EXTRA_ELAPSED_MILLIS = "extra_elapsed_millis";

    private static final String CHANNEL_ID = "voice_recording";
    private static final String CHANNEL_NAME = "Voice recording";
    private static final int NOTIFICATION_ID = 5201;
    private static final long TICK_MILLIS = 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final VoiceBinder binder = new VoiceBinder();

    private MediaRecorder recorder;
    private String noteId;
    private String noteTitle;
    private String outputPath;
    private long startedAt;
    private boolean recording;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!recording) {
                return;
            }
            sendTick();
            updateNotification();
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
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopRecording();
        }
        return START_NOT_STICKY;
    }

    public void startRecording(String noteId, String noteTitle) throws IOException {
        if (recording) {
            return;
        }
        this.noteId = noteId;
        this.noteTitle = isBlank(noteTitle) ? "StudyMate note" : noteTitle;
        this.outputPath = createOutputFile(noteId).getAbsolutePath();
        this.startedAt = System.currentTimeMillis();

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioEncodingBitRate(128000);
        recorder.setAudioSamplingRate(44100);
        recorder.setOutputFile(outputPath);
        recorder.prepare();
        recorder.start();

        recording = true;
        createChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        sendTick();
        handler.postDelayed(tickRunnable, TICK_MILLIS);
    }

    public RecordingResult stopRecording() {
        if (!recording) {
            return new RecordingResult(outputPath, 0L);
        }

        long duration = getElapsedMillis();
        recording = false;
        handler.removeCallbacks(tickRunnable);
        try {
            recorder.stop();
        } catch (RuntimeException ignored) {
            deleteBrokenRecording();
            outputPath = null;
            duration = 0L;
        } finally {
            recorder.release();
            recorder = null;
            stopForeground(true);
            stopSelf();
        }
        sendTick();
        return new RecordingResult(outputPath, duration);
    }

    public boolean isRecording() {
        return recording;
    }

    public String getNoteId() {
        return noteId;
    }

    public long getElapsedMillis() {
        return recording ? System.currentTimeMillis() - startedAt : 0L;
    }

    private File createOutputFile(String noteId) throws IOException {
        File dir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recordings");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Unable to create recordings folder");
        }
        String safeNoteId = noteId == null ? "note" : noteId.replaceAll("[^a-zA-Z0-9_-]", "_");
        return new File(dir, safeNoteId + "_" + System.currentTimeMillis() + ".m4a");
    }

    private Notification buildNotification() {
        Intent detailIntent = NoteDetailActivity.newIntent(this, noteId);
        detailIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                151,
                detailIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, VoiceRecordingService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                152,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        return builder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Recording: " + noteTitle)
                .setContentText(formatElapsed(getElapsedMillis()))
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .addAction(0, "Stop", stopPendingIntent)
                .build();
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null && recording) {
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private void sendTick() {
        Intent intent = new Intent(ACTION_TICK);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_NOTE_ID, noteId);
        intent.putExtra(EXTRA_ELAPSED_MILLIS, getElapsedMillis());
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
        channel.setDescription("Shows active StudyMate voice recordings");
        manager.createNotificationChannel(channel);
    }

    private void deleteBrokenRecording() {
        if (outputPath == null) {
            return;
        }
        File file = new File(outputPath);
        if (file.exists()) {
            file.delete();
        }
    }

    private String formatElapsed(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        return String.format(Locale.US, "%02d:%02d recording", totalSeconds / 60L, totalSeconds % 60L);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public class VoiceBinder extends Binder {
        public VoiceRecordingService getService() {
            return VoiceRecordingService.this;
        }
    }

    public static class RecordingResult {
        private final String path;
        private final long durationMillis;

        public RecordingResult(String path, long durationMillis) {
            this.path = path;
            this.durationMillis = durationMillis;
        }

        public String getPath() {
            return path;
        }

        public long getDurationMillis() {
            return durationMillis;
        }
    }
}
