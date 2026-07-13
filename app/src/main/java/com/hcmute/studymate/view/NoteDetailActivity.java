package com.hcmute.studymate.view;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hcmute.studymate.R;
import com.hcmute.studymate.controller.AuthController;
import com.hcmute.studymate.controller.NoteController;
import com.hcmute.studymate.controller.ReminderController;
import com.hcmute.studymate.controller.SummaryController;
import com.hcmute.studymate.controller.StudySessionController;
import com.hcmute.studymate.model.ChecklistItem;
import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.model.SummaryResult;
import com.hcmute.studymate.model.VoiceRecording;
import com.hcmute.studymate.service.FocusTimerService;
import com.hcmute.studymate.service.VoiceRecordingService;
import com.hcmute.studymate.utils.AppContainer;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.DateTimeUtils;
import com.hcmute.studymate.utils.OperationCallback;
import com.hcmute.studymate.utils.SummaryCallback;
import com.hcmute.studymate.utils.TagUtils;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class NoteDetailActivity extends AppCompatActivity {
    private static final String EXTRA_NOTE_ID = "extra_note_id";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1002;
    private static final long DEFAULT_FOCUS_DURATION = 25L * 60L * 1000L;

    private AuthController authController;
    private NoteController noteController;
    private ReminderController reminderController;
    private SummaryController summaryController;
    private StudySessionController studySessionController;
    private TextView titleText;
    private TextView categoryText;
    private TextView statusText;
    private TextView updatedAtText;
    private TextView tagsText;
    private TextView contentText;
    private TextView checklistTitleText;
    private LinearLayout checklistContainer;
    private TextView summaryTitleText;
    private TextView summaryText;
    private TextView reminderText;
    private TextView focusTimeText;
    private TextView recordingStatusText;
    private LinearLayout recordingsContainer;
    private MaterialButton pinButton;
    private MaterialButton startFocusButton;
    private MaterialButton pauseFocusButton;
    private MaterialButton stopFocusButton;
    private MaterialButton startRecordingButton;
    private MaterialButton stopRecordingButton;
    private Note currentNote;
    private String userId;
    private FocusTimerService focusTimerService;
    private boolean focusBound;
    private VoiceRecordingService voiceRecordingService;
    private boolean voiceBound;
    private MediaPlayer mediaPlayer;
    private String playingRecordingPath;

    public static Intent newIntent(Context context, String noteId) {
        Intent intent = new Intent(context, NoteDetailActivity.class);
        intent.putExtra(EXTRA_NOTE_ID, noteId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authController = AppContainer.authController();
        if (!requireLoggedInUser()) {
            return;
        }
        setContentView(R.layout.activity_note_detail);

        noteController = AppContainer.noteController();
        reminderController = AppContainer.reminderController();
        summaryController = AppContainer.summaryController();
        studySessionController = AppContainer.studySessionController();

        titleText = findViewById(R.id.detailTitleText);
        categoryText = findViewById(R.id.detailCategoryText);
        statusText = findViewById(R.id.detailStatusText);
        updatedAtText = findViewById(R.id.detailUpdatedAtText);
        tagsText = findViewById(R.id.detailTagsText);
        contentText = findViewById(R.id.detailContentText);
        checklistTitleText = findViewById(R.id.detailChecklistTitleText);
        checklistContainer = findViewById(R.id.detailChecklistContainer);
        summaryTitleText = findViewById(R.id.detailSummaryTitleText);
        summaryText = findViewById(R.id.detailSummaryText);
        reminderText = findViewById(R.id.detailReminderText);
        focusTimeText = findViewById(R.id.focusTimeText);
        recordingStatusText = findViewById(R.id.recordingStatusText);
        recordingsContainer = findViewById(R.id.recordingsContainer);
        MaterialButton editButton = findViewById(R.id.editNoteButton);
        MaterialButton deleteButton = findViewById(R.id.deleteNoteButton);
        MaterialButton summarizeButton = findViewById(R.id.summarizeNoteButton);
        MaterialButton reminderButton = findViewById(R.id.reminderNoteButton);
        pinButton = findViewById(R.id.pinNoteButton);
        startFocusButton = findViewById(R.id.startFocusButton);
        pauseFocusButton = findViewById(R.id.pauseFocusButton);
        stopFocusButton = findViewById(R.id.stopFocusButton);
        startRecordingButton = findViewById(R.id.startRecordingButton);
        stopRecordingButton = findViewById(R.id.stopRecordingButton);

        editButton.setOnClickListener(view -> {
            if (currentNote != null) {
                startActivity(NoteEditActivity.newIntent(this, currentNote.getId()));
            }
        });
        pinButton.setOnClickListener(view -> togglePinned());
        deleteButton.setOnClickListener(view -> confirmDelete());
        summarizeButton.setOnClickListener(view -> summarizeCurrentNote());
        reminderButton.setOnClickListener(view -> showReminderPicker());
        startFocusButton.setOnClickListener(view -> startFocusSession());
        pauseFocusButton.setOnClickListener(view -> toggleFocusPause());
        stopFocusButton.setOnClickListener(view -> stopFocusSession());
        startRecordingButton.setOnClickListener(view -> startVoiceRecording());
        stopRecordingButton.setOnClickListener(view -> stopVoiceRecording());
        requestNotificationPermissionIfNeeded();
        renderFocusState(DEFAULT_FOCUS_DURATION, false, false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, FocusTimerService.class);
        bindService(intent, focusConnection, Context.BIND_AUTO_CREATE);
        bindService(new Intent(this, VoiceRecordingService.class), voiceConnection, Context.BIND_AUTO_CREATE);
        registerFocusReceiver();
        registerVoiceReceiver();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (focusBound) {
            unbindService(focusConnection);
            focusBound = false;
        }
        if (voiceBound) {
            unbindService(voiceConnection);
            voiceBound = false;
        }
        unregisterReceiver(focusReceiver);
        unregisterReceiver(voiceReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!requireLoggedInUser()) {
            return;
        }
        String noteId = getIntent().getStringExtra(EXTRA_NOTE_ID);
        if (noteId == null) {
            finish();
            return;
        }
        loadNote(noteId);
    }

    private void loadNote(String noteId) {
        noteController.loadNote(userId, noteId, new DataCallback<Note>() {
            @Override
            public void onSuccess(Note data) {
                currentNote = data;
                renderNote();
            }

            @Override
            public void onError(Exception exception) {
                showError("Could not load this note", exception);
                finish();
            }
        });
    }

    private void renderNote() {
        titleText.setText(currentNote.getTitle());
        categoryText.setText(currentNote.getCategory());
        statusText.setText(normalizeStatus(currentNote.getStatus()));
        updatedAtText.setText("Updated " + DateTimeUtils.formatDateTime(currentNote.getUpdatedAt()));
        contentText.setText(currentNote.getContent());

        String tags = TagUtils.join(currentNote.getTags());
        tagsText.setVisibility(tags.isEmpty() ? View.GONE : View.VISIBLE);
        tagsText.setText(tags.isEmpty() ? "" : "#" + tags.replace(", ", " #"));

        boolean hasSummary = currentNote.getSummary() != null && !currentNote.getSummary().trim().isEmpty();
        summaryTitleText.setVisibility(hasSummary ? View.VISIBLE : View.GONE);
        summaryText.setVisibility(hasSummary ? View.VISIBLE : View.GONE);
        summaryText.setText(hasSummary ? currentNote.getSummary() : "");
        pinButton.setText(currentNote.isPinned() ? R.string.unpin_note : R.string.pin_note);
        renderChecklist();
        renderRecordingState();

        if (currentNote.getReminderAt() == null) {
            reminderText.setText("Reminder not set");
        } else {
            reminderText.setText("Reminder " + DateTimeUtils.formatDateTime(currentNote.getReminderAt()));
        }
        syncFocusFromService();
    }

    private void renderChecklist() {
        checklistContainer.removeAllViews();
        List<ChecklistItem> items = currentNote.getChecklist();
        boolean hasChecklist = items != null && !items.isEmpty();
        checklistTitleText.setVisibility(hasChecklist ? View.VISIBLE : View.GONE);
        checklistContainer.setVisibility(hasChecklist ? View.VISIBLE : View.GONE);
        if (!hasChecklist) {
            return;
        }

        for (int index = 0; index < items.size(); index++) {
            ChecklistItem item = items.get(index);
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(item.getText());
            checkBox.setTextColor(getColor(R.color.studymate_text_primary));
            checkBox.setTextSize(15f);
            checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.studymate_primary)));
            checkBox.setChecked(item.isChecked());
            int itemIndex = index;
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> updateChecklistItem(itemIndex, isChecked));
            checklistContainer.addView(checkBox);
        }
    }

    private void updateChecklistItem(int index, boolean checked) {
        if (currentNote == null || currentNote.getChecklist() == null
                || index < 0 || index >= currentNote.getChecklist().size()) {
            return;
        }
        currentNote.getChecklist().get(index).setChecked(checked);
        noteController.saveNote(userId, currentNote, new OperationCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(NoteDetailActivity.this, "Checklist updated", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception exception) {
                showError("Could not update checklist", exception);
                renderChecklist();
            }
        });
    }

    private void togglePinned() {
        if (currentNote == null) {
            return;
        }
        boolean nextPinned = !currentNote.isPinned();
        pinButton.setEnabled(false);
        noteController.setPinned(userId, currentNote, nextPinned, new OperationCallback() {
            @Override
            public void onSuccess() {
                currentNote.setPinned(nextPinned);
                pinButton.setEnabled(true);
                renderNote();
                Toast.makeText(NoteDetailActivity.this,
                        nextPinned ? "Note pinned" : "Note unpinned",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception exception) {
                pinButton.setEnabled(true);
                showError("Could not update pin", exception);
            }
        });
    }

    private void summarizeCurrentNote() {
        if (currentNote == null) {
            return;
        }
        summaryController.summarize(currentNote.getContent(), new SummaryCallback() {
            @Override
            public void onSuccess(SummaryResult result) {
                currentNote.setSummary(result.getSummaryText());
                noteController.saveNote(userId, currentNote, new OperationCallback() {
                    @Override
                    public void onSuccess() {
                        renderNote();
                        Toast.makeText(NoteDetailActivity.this, "Summary generated", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception exception) {
                        showError("Could not save summary", exception);
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                showError("Could not generate summary", exception);
            }
        });
    }

    private void showReminderPicker() {
        if (currentNote == null) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        if (currentNote.getReminderAt() != null && currentNote.getReminderAt() > System.currentTimeMillis()) {
            calendar.setTimeInMillis(currentNote.getReminderAt());
        } else {
            calendar.add(Calendar.HOUR_OF_DAY, 1);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    showReminderTimePicker(calendar);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showReminderTimePicker(Calendar calendar) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    saveReminder(calendar.getTimeInMillis());
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    private void saveReminder(long reminderAt) {
        if (reminderAt <= System.currentTimeMillis()) {
            Toast.makeText(this, "Choose a future reminder time", Toast.LENGTH_SHORT).show();
            return;
        }

        reminderController.setReminder(this, userId, currentNote, reminderAt, new OperationCallback() {
            @Override
            public void onSuccess() {
                currentNote.setReminderAt(reminderAt);
                renderNote();
                Toast.makeText(NoteDetailActivity.this, "Reminder saved", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception exception) {
                showError("Could not save reminder", exception);
            }
        });
    }

    private void startFocusSession() {
        if (currentNote == null) {
            return;
        }
        Intent intent = new Intent(this, FocusTimerService.class);
        intent.setAction(FocusTimerService.ACTION_START);
        intent.putExtra(FocusTimerService.EXTRA_NOTE_ID, currentNote.getId());
        intent.putExtra(FocusTimerService.EXTRA_NOTE_TITLE, currentNote.getTitle());
        intent.putExtra(FocusTimerService.EXTRA_DURATION_MILLIS, DEFAULT_FOCUS_DURATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        renderFocusState(DEFAULT_FOCUS_DURATION, true, true);
    }

    private void toggleFocusPause() {
        if (focusTimerService == null || !focusTimerService.hasActiveSession()) {
            return;
        }
        if (focusTimerService.isRunning()) {
            focusTimerService.pauseSession();
        } else {
            focusTimerService.resumeSession();
        }
        syncFocusFromService();
    }

    private void stopFocusSession() {
        if (focusTimerService != null) {
            focusTimerService.stopSession();
        } else {
            Intent intent = new Intent(this, FocusTimerService.class);
            intent.setAction(FocusTimerService.ACTION_STOP);
            startService(intent);
        }
        renderFocusState(DEFAULT_FOCUS_DURATION, false, false);
    }

    private void startVoiceRecording() {
        if (currentNote == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }
        if (voiceRecordingService == null) {
            Toast.makeText(this, "Recording service is starting. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            voiceRecordingService.startRecording(currentNote.getId(), currentNote.getTitle());
            renderRecordingActive(0L);
        } catch (Exception exception) {
            showError("Could not start recording", exception);
        }
    }

    private void stopVoiceRecording() {
        if (voiceRecordingService == null || !voiceRecordingService.isRecording()) {
            renderRecordingState();
            return;
        }
        VoiceRecordingService.RecordingResult result = voiceRecordingService.stopRecording();
        if (result.getPath() == null || result.getDurationMillis() <= 0L) {
            Toast.makeText(this, "Recording was too short", Toast.LENGTH_SHORT).show();
            renderRecordingState();
            return;
        }
        VoiceRecording recording = new VoiceRecording(
                result.getPath(),
                result.getDurationMillis(),
                System.currentTimeMillis(),
                "Voice note " + (getRecordingsForDisplay().size() + 1)
        );
        currentNote.getRecordings().add(0, recording);
        currentNote.setRecordingPath(result.getPath());
        currentNote.setRecordingDurationMillis(result.getDurationMillis());
        noteController.saveNote(userId, currentNote, new OperationCallback() {
            @Override
            public void onSuccess() {
                renderRecordingState();
                Toast.makeText(NoteDetailActivity.this, "Voice note saved", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception exception) {
                showError("Could not save voice note", exception);
            }
        });
    }

    private void playVoiceRecording(String path) {
        if (path == null || path.trim().isEmpty()) {
            Toast.makeText(this, "No voice note to play", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying() && path.equals(playingRecordingPath)) {
                releaseMediaPlayer();
                renderRecordingsList();
                return;
            }
            releaseMediaPlayer();
            mediaPlayer = new MediaPlayer();
            playingRecordingPath = path;
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(player -> {
                player.release();
                mediaPlayer = null;
                playingRecordingPath = null;
                renderRecordingsList();
            });
            mediaPlayer.start();
            renderRecordingsList();
        } catch (Exception exception) {
            showError("Could not play voice note", exception);
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.release();
        mediaPlayer = null;
        playingRecordingPath = null;
    }

    private void renderRecordingState() {
        boolean recording = voiceRecordingService != null
                && voiceRecordingService.isRecording()
                && currentNote != null
                && currentNote.getId().equals(voiceRecordingService.getNoteId());
        if (recording) {
            renderRecordingActive(voiceRecordingService.getElapsedMillis());
            return;
        }

        startRecordingButton.setVisibility(View.VISIBLE);
        stopRecordingButton.setVisibility(View.GONE);
        if (hasRecording()) {
            int count = getRecordingsForDisplay().size();
            recordingStatusText.setText(count + (count == 1 ? " voice note saved" : " voice notes saved"));
        } else {
            recordingStatusText.setText("No recording yet");
        }
        renderRecordingsList();
    }

    private void renderRecordingActive(long elapsedMillis) {
        startRecordingButton.setVisibility(View.GONE);
        stopRecordingButton.setVisibility(View.VISIBLE);
        recordingStatusText.setText("Recording " + formatVoiceTime(elapsedMillis));
    }

    private boolean hasRecording() {
        return !getRecordingsForDisplay().isEmpty();
    }

    private List<VoiceRecording> getRecordingsForDisplay() {
        List<VoiceRecording> recordings = new java.util.ArrayList<>();
        if (currentNote == null) {
            return recordings;
        }
        if (currentNote.getRecordings() != null) {
            recordings.addAll(currentNote.getRecordings());
        }
        if (recordings.isEmpty()
                && currentNote.getRecordingPath() != null
                && !currentNote.getRecordingPath().trim().isEmpty()) {
            recordings.add(new VoiceRecording(
                    currentNote.getRecordingPath(),
                    currentNote.getRecordingDurationMillis(),
                    currentNote.getUpdatedAt(),
                    "Voice note 1"
            ));
        }
        return recordings;
    }

    private VoiceRecording getLatestRecording() {
        List<VoiceRecording> recordings = getRecordingsForDisplay();
        return recordings.isEmpty() ? null : recordings.get(0);
    }

    private void renderRecordingsList() {
        if (recordingsContainer == null) {
            return;
        }
        recordingsContainer.removeAllViews();
        List<VoiceRecording> recordings = getRecordingsForDisplay();
        for (int index = 0; index < recordings.size(); index++) {
            recordingsContainer.addView(buildRecordingRow(recordings.get(index), index + 1));
        }
    }

    private View buildRecordingRow(VoiceRecording recording, int number) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, getResources().getDimensionPixelSize(R.dimen.space_sm), 0, 0);
        row.setBackgroundResource(R.drawable.bg_auth_panel);

        TextView label = new TextView(this);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        label.setLayoutParams(labelParams);
        String name = recording.getLabel() == null || recording.getLabel().trim().isEmpty()
                ? "Voice note " + number
                : recording.getLabel();
        label.setText(name + " - " + formatVoiceTime(recording.getDurationMillis()));
        label.setTextColor(getColor(R.color.studymate_text_primary));
        label.setTextSize(14f);
        label.setOnClickListener(view -> showRenameRecordingDialog(recording, name));

        MaterialButton playButton = new MaterialButton(this);
        playButton.setText(recording.getPath() != null && recording.getPath().equals(playingRecordingPath) ? "Stop" : "Play");
        playButton.setOnClickListener(view -> playVoiceRecording(recording.getPath()));

        MaterialButton deleteButton = new MaterialButton(this);
        deleteButton.setText("Delete");
        deleteButton.setVisibility(View.GONE);
        deleteButton.setTextColor(getColor(R.color.studymate_danger));
        deleteButton.setOnClickListener(view -> confirmDeleteRecording(recording));

        final float[] downX = new float[1];
        row.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downX[0] = event.getX();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float deltaX = event.getX() - downX[0];
                if (deltaX < -80f) {
                    deleteButton.setVisibility(View.VISIBLE);
                    return true;
                }
                if (deltaX > 80f) {
                    deleteButton.setVisibility(View.GONE);
                    return true;
                }
                view.performClick();
            }
            return true;
        });

        row.addView(label);
        row.addView(playButton);
        row.addView(deleteButton);
        return row;
    }

    private void confirmDeleteRecording(VoiceRecording recording) {
        new AlertDialog.Builder(this)
                .setTitle("Delete voice note?")
                .setMessage("This recording will be removed from this note.")
                .setPositiveButton("Delete", (dialog, which) -> deleteRecording(recording))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRenameRecordingDialog(VoiceRecording recording, String currentName) {
        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setHint("Recording name");
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_FILLED);
        int padding = getResources().getDimensionPixelSize(R.dimen.space_lg);
        inputLayout.setPadding(padding, 0, padding, 0);

        TextInputEditText input = new TextInputEditText(this);
        input.setText(currentName);
        input.selectAll();
        inputLayout.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Rename voice note")
                .setView(inputLayout)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(openDialog -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    String newName = input.getText() == null ? "" : input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        inputLayout.setError("Name is required");
                        return;
                    }
                    inputLayout.setError(null);
                    renameRecording(recording, newName, dialog);
                }));
        dialog.show();
    }

    private void renameRecording(VoiceRecording recording, String newName, AlertDialog dialog) {
        if (currentNote == null || recording == null || recording.getPath() == null) {
            return;
        }
        if (currentNote.getRecordings() != null) {
            for (VoiceRecording item : currentNote.getRecordings()) {
                if (recording.getPath().equals(item.getPath())) {
                    item.setLabel(newName);
                    break;
                }
            }
        }
        noteController.saveNote(userId, currentNote, new OperationCallback() {
            @Override
            public void onSuccess() {
                dialog.dismiss();
                renderRecordingState();
                Toast.makeText(NoteDetailActivity.this, "Voice note renamed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception exception) {
                showError("Could not rename voice note", exception);
            }
        });
    }

    private void deleteRecording(VoiceRecording recording) {
        if (currentNote == null || recording == null || recording.getPath() == null) {
            return;
        }
        if (recording.getPath().equals(playingRecordingPath)) {
            releaseMediaPlayer();
        }
        if (currentNote.getRecordings() != null) {
            currentNote.getRecordings().removeIf(item -> recording.getPath().equals(item.getPath()));
        }
        if (recording.getPath().equals(currentNote.getRecordingPath())) {
            VoiceRecording latest = getLatestRecording();
            if (latest == null || recording.getPath().equals(latest.getPath())) {
                currentNote.setRecordingPath(null);
                currentNote.setRecordingDurationMillis(0L);
            } else {
                currentNote.setRecordingPath(latest.getPath());
                currentNote.setRecordingDurationMillis(latest.getDurationMillis());
            }
        }
        java.io.File file = new java.io.File(recording.getPath());
        if (file.exists()) {
            file.delete();
        }
        noteController.saveNote(userId, currentNote, new OperationCallback() {
            @Override
            public void onSuccess() {
                renderRecordingState();
                Toast.makeText(NoteDetailActivity.this, "Voice note deleted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception exception) {
                showError("Could not delete voice note", exception);
            }
        });
    }

    private String formatVoiceTime(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        return String.format(Locale.US, "%02d:%02d", totalSeconds / 60L, totalSeconds % 60L);
    }

    private void syncFocusFromService() {
        if (focusTimerService == null || !focusTimerService.hasActiveSession()) {
            renderFocusState(DEFAULT_FOCUS_DURATION, false, false);
            return;
        }
        boolean sameNote = currentNote != null && currentNote.getId().equals(focusTimerService.getNoteId());
        renderFocusState(
                sameNote ? focusTimerService.getRemainingMillis() : DEFAULT_FOCUS_DURATION,
                sameNote,
                focusTimerService.isRunning()
        );
    }

    private void renderFocusState(long remainingMillis, boolean active, boolean running) {
        focusTimeText.setText(formatFocusTime(remainingMillis));
        startFocusButton.setVisibility(active ? View.GONE : View.VISIBLE);
        pauseFocusButton.setVisibility(active ? View.VISIBLE : View.GONE);
        stopFocusButton.setVisibility(active ? View.VISIBLE : View.GONE);
        pauseFocusButton.setText(running ? "Pause" : "Resume");
    }

    private void handleFocusFinished(Intent intent) {
        if (currentNote == null) {
            return;
        }
        String finishedNoteId = intent.getStringExtra(FocusTimerService.EXTRA_NOTE_ID);
        if (!currentNote.getId().equals(finishedNoteId)) {
            return;
        }
        long durationMillis = intent.getLongExtra(FocusTimerService.EXTRA_DURATION_MILLIS, DEFAULT_FOCUS_DURATION);
        studySessionController.saveCompletedSession(userId, currentNote, durationMillis, new OperationCallback() {
            @Override
            public void onSuccess() {
                renderFocusState(DEFAULT_FOCUS_DURATION, false, false);
                Toast.makeText(NoteDetailActivity.this, "Focus session saved", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception exception) {
                renderFocusState(DEFAULT_FOCUS_DURATION, false, false);
                showError("Could not save focus session", exception);
            }
        });
    }

    private String formatFocusTime(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds);
    }

    private String normalizeStatus(String status) {
        return status == null || status.trim().isEmpty() ? Constants.STATUS_NEW : status;
    }

    private void confirmDelete() {
        if (currentNote == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Delete note")
                .setMessage("This note will be permanently removed from your StudyMate notes.")
                .setPositiveButton("Delete", (dialog, which) -> deleteCurrentNote())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCurrentNote() {
        noteController.deleteNote(userId, currentNote.getId(), new OperationCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(NoteDetailActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception exception) {
                showError("Could not delete note", exception);
            }
        });
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
    }

    private boolean requireLoggedInUser() {
        userId = authController.getCurrentUserId();
        if (userId != null && !userId.trim().isEmpty()) {
            return true;
        }
        openLoginAndFinish();
        return false;
    }

    private void openLoginAndFinish() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecording();
            } else {
                Toast.makeText(this, "Microphone permission is required for voice notes", Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (requestCode != REQUEST_NOTIFICATION_PERMISSION) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Reminder notifications enabled", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Reminders will be saved, but notifications are disabled", Toast.LENGTH_LONG).show();
        }
    }

    private void showError(String prefix, Exception exception) {
        String detail = exception == null || exception.getMessage() == null
                ? "Please check your connection and Firestore rules."
                : exception.getMessage();
        Toast.makeText(this, prefix + ". " + detail, Toast.LENGTH_LONG).show();
    }

    private void registerFocusReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(FocusTimerService.ACTION_TICK);
        filter.addAction(FocusTimerService.ACTION_FINISHED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(focusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(focusReceiver, filter);
        }
    }

    private void registerVoiceReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(VoiceRecordingService.ACTION_TICK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(voiceReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(voiceReceiver, filter);
        }
    }

    private final ServiceConnection focusConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FocusTimerService.FocusBinder binder = (FocusTimerService.FocusBinder) service;
            focusTimerService = binder.getService();
            focusBound = true;
            syncFocusFromService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            focusTimerService = null;
            focusBound = false;
        }
    };

    private final ServiceConnection voiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            VoiceRecordingService.VoiceBinder binder = (VoiceRecordingService.VoiceBinder) service;
            voiceRecordingService = binder.getService();
            voiceBound = true;
            renderRecordingState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            voiceRecordingService = null;
            voiceBound = false;
        }
    };

    private final BroadcastReceiver focusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || currentNote == null) {
                return;
            }
            String noteId = intent.getStringExtra(FocusTimerService.EXTRA_NOTE_ID);
            if (!currentNote.getId().equals(noteId)) {
                return;
            }
            if (FocusTimerService.ACTION_FINISHED.equals(intent.getAction())) {
                handleFocusFinished(intent);
                return;
            }
            long remainingMillis = intent.getLongExtra(FocusTimerService.EXTRA_REMAINING_MILLIS, DEFAULT_FOCUS_DURATION);
            boolean running = intent.getBooleanExtra(FocusTimerService.EXTRA_RUNNING, false);
            renderFocusState(remainingMillis, remainingMillis > 0L, running);
        }
    };

    private final BroadcastReceiver voiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || currentNote == null) {
                return;
            }
            String noteId = intent.getStringExtra(VoiceRecordingService.EXTRA_NOTE_ID);
            if (!currentNote.getId().equals(noteId)) {
                return;
            }
            long elapsedMillis = intent.getLongExtra(VoiceRecordingService.EXTRA_ELAPSED_MILLIS, 0L);
            renderRecordingActive(elapsedMillis);
        }
    };
}
