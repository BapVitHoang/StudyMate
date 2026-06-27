package com.hcmute.studymate.repository;

import com.hcmute.studymate.model.Reminder;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.OperationCallback;

public interface ReminderRepository {
    void saveReminder(String userId, Reminder reminder, OperationCallback callback);

    void getReminderForNote(String userId, String noteId, DataCallback<Reminder> callback);
}
