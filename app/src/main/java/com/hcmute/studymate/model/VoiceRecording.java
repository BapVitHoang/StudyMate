package com.hcmute.studymate.model;

public class VoiceRecording {
    private String path;
    private long durationMillis;
    private long createdAt;
    private String label;

    public VoiceRecording() {
    }

    public VoiceRecording(String path, long durationMillis, long createdAt, String label) {
        this.path = path;
        this.durationMillis = durationMillis;
        this.createdAt = createdAt;
        this.label = label;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
