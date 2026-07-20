package com.hcmute.studymate.model;

public class RagCitation {
    private String noteId;
    private String title;
    private String excerpt;

    public RagCitation() {
    }

    public RagCitation(String noteId, String title, String excerpt) {
        this.noteId = noteId;
        this.title = title;
        this.excerpt = excerpt;
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

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }
}
