package com.hcmute.studymate.view;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.hcmute.studymate.R;
import com.hcmute.studymate.controller.AuthController;
import com.hcmute.studymate.controller.NoteController;
import com.hcmute.studymate.controller.ReminderController;
import com.hcmute.studymate.controller.SummaryController;
import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.model.SummaryResult;
import com.hcmute.studymate.utils.AppContainer;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.DateTimeUtils;
import com.hcmute.studymate.utils.OperationCallback;
import com.hcmute.studymate.utils.SummaryCallback;
import com.hcmute.studymate.utils.TagUtils;

import java.util.Calendar;

public class NoteDetailActivity extends AppCompatActivity {
    private static final String EXTRA_NOTE_ID = "extra_note_id";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;

    private AuthController authController;
    private NoteController noteController;
    private ReminderController reminderController;
    private SummaryController summaryController;
    private TextView titleText;
    private TextView categoryText;
    private TextView updatedAtText;
    private TextView tagsText;
    private TextView contentText;
    private TextView summaryTitleText;
    private TextView summaryText;
    private TextView reminderText;
    private Note currentNote;
    private String userId;

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

        titleText = findViewById(R.id.detailTitleText);
        categoryText = findViewById(R.id.detailCategoryText);
        updatedAtText = findViewById(R.id.detailUpdatedAtText);
        tagsText = findViewById(R.id.detailTagsText);
        contentText = findViewById(R.id.detailContentText);
        summaryTitleText = findViewById(R.id.detailSummaryTitleText);
        summaryText = findViewById(R.id.detailSummaryText);
        reminderText = findViewById(R.id.detailReminderText);
        MaterialButton editButton = findViewById(R.id.editNoteButton);
        MaterialButton deleteButton = findViewById(R.id.deleteNoteButton);
        MaterialButton summarizeButton = findViewById(R.id.summarizeNoteButton);
        MaterialButton reminderButton = findViewById(R.id.reminderNoteButton);

        editButton.setOnClickListener(view -> {
            if (currentNote != null) {
                startActivity(NoteEditActivity.newIntent(this, currentNote.getId()));
            }
        });
        deleteButton.setOnClickListener(view -> confirmDelete());
        summarizeButton.setOnClickListener(view -> summarizeCurrentNote());
        reminderButton.setOnClickListener(view -> showReminderPicker());
        requestNotificationPermissionIfNeeded();
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
        updatedAtText.setText("Updated " + DateTimeUtils.formatDateTime(currentNote.getUpdatedAt()));
        contentText.setText(currentNote.getContent());

        String tags = TagUtils.join(currentNote.getTags());
        tagsText.setVisibility(tags.isEmpty() ? View.GONE : View.VISIBLE);
        tagsText.setText(tags.isEmpty() ? "" : "#" + tags.replace(", ", " #"));

        boolean hasSummary = currentNote.getSummary() != null && !currentNote.getSummary().trim().isEmpty();
        summaryTitleText.setVisibility(hasSummary ? View.VISIBLE : View.GONE);
        summaryText.setVisibility(hasSummary ? View.VISIBLE : View.GONE);
        summaryText.setText(hasSummary ? currentNote.getSummary() : "");

        if (currentNote.getReminderAt() == null) {
            reminderText.setText("Reminder not set");
        } else {
            reminderText.setText("Reminder " + DateTimeUtils.formatDateTime(currentNote.getReminderAt()));
        }
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
}
