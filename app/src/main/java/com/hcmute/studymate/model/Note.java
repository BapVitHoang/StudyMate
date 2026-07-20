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
    private List<ChecklistItem> checklist = new ArrayList<>();
    private long createdAt;
    private long updatedAt;
    private Long reminderAt;
    private String summary;
    private boolean pinned;
    private String status;
    private String recordingPath;
    private long recordingDurationMillis;
    private List<VoiceRecording> recordings = new ArrayList<>();
    private List<String> summaryBullets = new ArrayList<>();
    private List<String> summaryKeyTerms = new ArrayList<>();
    private Double summaryConfidence;
    private String summarySource;
    private Long summaryGeneratedAt;

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

    public List<ChecklistItem> getChecklist() {
        return checklist;
    }

    public void setChecklist(List<ChecklistItem> checklist) {
        this.checklist = checklist == null ? new ArrayList<>() : checklist;
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

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRecordingPath() {
        return recordingPath;
    }

    public void setRecordingPath(String recordingPath) {
        this.recordingPath = recordingPath;
    }

    public long getRecordingDurationMillis() {
        return recordingDurationMillis;
    }

    public void setRecordingDurationMillis(long recordingDurationMillis) {
        this.recordingDurationMillis = recordingDurationMillis;
    }

    public List<VoiceRecording> getRecordings() {
        return recordings;
    }

    public void setRecordings(List<VoiceRecording> recordings) {
        this.recordings = recordings == null ? new ArrayList<>() : recordings;
    }

    public List<String> getSummaryBullets() {
        return summaryBullets;
    }

    public void setSummaryBullets(List<String> summaryBullets) {
        this.summaryBullets = summaryBullets == null ? new ArrayList<>() : summaryBullets;
    }

    public List<String> getSummaryKeyTerms() {
        return summaryKeyTerms;
    }

    public void setSummaryKeyTerms(List<String> summaryKeyTerms) {
        this.summaryKeyTerms = summaryKeyTerms == null ? new ArrayList<>() : summaryKeyTerms;
    }

    public Double getSummaryConfidence() {
        return summaryConfidence;
    }

    public void setSummaryConfidence(Double summaryConfidence) {
        this.summaryConfidence = summaryConfidence;
    }

    public String getSummarySource() {
        return summarySource;
    }

    public void setSummarySource(String summarySource) {
        this.summarySource = summarySource;
    }

    public Long getSummaryGeneratedAt() {
        return summaryGeneratedAt;
    }

    public void setSummaryGeneratedAt(Long summaryGeneratedAt) {
        this.summaryGeneratedAt = summaryGeneratedAt;
    }
}
