package com.hcmute.studymate.service;

import android.content.Context;

import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.model.Reminder;
import com.hcmute.studymate.repository.NoteRepository;
import com.hcmute.studymate.repository.ReminderRepository;
import com.hcmute.studymate.utils.NotificationHelper;
import com.hcmute.studymate.utils.OperationCallback;

public class ReminderService {
    private final ReminderRepository reminderRepository;
    private final NoteRepository noteRepository;

    public ReminderService(ReminderRepository reminderRepository, NoteRepository noteRepository) {
        this.reminderRepository = reminderRepository;
        this.noteRepository = noteRepository;
    }

    public void setReminder(Context context, String userId, Note note, long reminderAt, OperationCallback callback) {
        if (note == null || note.getId() == null) {
            callback.onError(new IllegalArgumentException("A saved note is required before setting a reminder"));
            return;
        }
        if (reminderAt <= System.currentTimeMillis()) {
            callback.onError(new IllegalArgumentException("Reminder time must be in the future"));
            return;
        }

        Reminder reminder = new Reminder(note.getId(), userId, note.getId(), reminderAt, true);
        reminderRepository.saveReminder(userId, reminder, new OperationCallback() {
            @Override
            public void onSuccess() {
                note.setReminderAt(reminderAt);
                note.setUpdatedAt(System.currentTimeMillis());
                noteRepository.saveNote(userId, note, new OperationCallback() {
                    @Override
                    public void onSuccess() {
                        NotificationHelper.scheduleReminder(context, reminder, note);
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(Exception exception) {
                        callback.onError(exception);
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }
}
