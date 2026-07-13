package com.hcmute.studymate.model;

public class StudySession {
    private String id;
    private String userId;
    private String noteId;
    private String noteTitle;
    private String category;
    private long durationMillis;
    private long completedAt;

    public StudySession() {
    }

    public StudySession(String id, String userId, String noteId, String noteTitle,
                        String category, long durationMillis, long completedAt) {
        this.id = id;
        this.userId = userId;
        this.noteId = noteId;
        this.noteTitle = noteTitle;
        this.category = category;
        this.durationMillis = durationMillis;
        this.completedAt = completedAt;
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

    public String getNoteTitle() {
        return noteTitle;
    }

    public void setNoteTitle(String noteTitle) {
        this.noteTitle = noteTitle;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }
}
