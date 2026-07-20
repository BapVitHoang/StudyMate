package com.hcmute.studymate.model;

import java.util.ArrayList;
import java.util.List;

public class TutorSession {
    private String id;
    private String title;
    private List<TutorMessage> messages = new ArrayList<>();
    private long updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<TutorMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<TutorMessage> messages) {
        this.messages = messages == null ? new ArrayList<>() : messages;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
