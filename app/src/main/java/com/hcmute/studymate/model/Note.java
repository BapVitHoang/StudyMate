package com.hcmute.studymate.model;

import java.util.ArrayList;
import java.util.List;

public class Note {
    private String id;
    private String userId;
    private String title;
    private String content;
    private String category;
    private List<String> tags = new ArrayList<>();
    private long createdAt;
    private long updatedAt;
    private Long reminderAt;
    private String summary;

    public Note() {
    }

    public Note(String id, String userId, String title, String content, String category) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.category = category;
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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : tags;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getReminderAt() {
        return reminderAt;
    }

    public void setReminderAt(Long reminderAt) {
        this.reminderAt = reminderAt;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
