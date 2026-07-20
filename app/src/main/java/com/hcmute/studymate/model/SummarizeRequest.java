package com.hcmute.studymate.model;

public class SummarizeRequest {
    private String noteId;
    private String title;
    private String content;
    private String category;
    private String locale;

    public SummarizeRequest() {
    }

    public SummarizeRequest(String noteId, String title, String content, String category, String locale) {
        this.noteId = noteId;
        this.title = title;
        this.content = content;
        this.category = category;
        this.locale = locale;
    }

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
