package com.hcmute.studymate.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hcmute.studymate.model.Reminder;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.OperationCallback;

public class FirestoreReminderRepository implements ReminderRepository {
    private static final String USERS_COLLECTION = "users";
    private static final String REMINDERS_COLLECTION = "reminders";

    private final FirebaseFirestore firestore;

    public FirestoreReminderRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreReminderRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void saveReminder(String userId, Reminder reminder, OperationCallback callback) {
        if (isBlank(userId) || reminder == null || isBlank(reminder.getNoteId())) {
            callback.onError(new IllegalArgumentException("User id and reminder note id are required"));
            return;
        }

        reminder.setUserId(userId);
        reminder.setId(reminder.getNoteId());

        remindersRef(userId)
                .document(reminder.getNoteId())
                .set(reminder)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void getReminderForNote(String userId, String noteId, DataCallback<Reminder> callback) {
        if (isBlank(userId) || isBlank(noteId)) {
            callback.onError(new IllegalArgumentException("User id and note id are required"));
            return;
        }

        remindersRef(userId)
                .document(noteId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onError(new IllegalArgumentException("Reminder not found"));
                        return;
                    }

                    Reminder reminder = document.toObject(Reminder.class);
                    if (reminder == null) {
                        callback.onError(new IllegalStateException("Unable to read reminder data"));
                        return;
                    }

                    if (isBlank(reminder.getId())) {
                        reminder.setId(document.getId());
                    }
                    if (isBlank(reminder.getUserId())) {
                        reminder.setUserId(userId);
                    }
                    callback.onSuccess(reminder);
                })
                .addOnFailureListener(callback::onError);
    }

    private CollectionReference remindersRef(String userId) {
        return firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(REMINDERS_COLLECTION);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
