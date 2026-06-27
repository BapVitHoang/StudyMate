package com.hcmute.studymate.model;

public class Reminder {
    private String id;
    private String userId;
    private String noteId;
    private long reminderAt;
    private boolean enabled;

    public Reminder() {
    }

    public Reminder(String id, String userId, String noteId, long reminderAt, boolean enabled) {
        this.id = id;
        this.userId = userId;
        this.noteId = noteId;
        this.reminderAt = reminderAt;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public long getReminderAt() {
        return reminderAt;
    }

    public void setReminderAt(long reminderAt) {
        this.reminderAt = reminderAt;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
