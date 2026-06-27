package com.hcmute.studymate.controller;

import android.content.Context;

import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.service.ReminderService;
import com.hcmute.studymate.utils.OperationCallback;

public class ReminderController {
    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    public void setReminder(Context context, String userId, Note note, long reminderAt, OperationCallback callback) {
        reminderService.setReminder(context, userId, note, reminderAt, callback);
    }
}
