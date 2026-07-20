package com.hcmute.studymate.model;

import java.util.ArrayList;
import java.util.List;

public class LearningGap {
    private String id;
    private String conceptName;
    private String reason;
    private double severity;
    private List<String> relatedNoteIds = new ArrayList<>();
    private long updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConceptName() {
        return conceptName;
    }

    public void setConceptName(String conceptName) {
        this.conceptName = conceptName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public double getSeverity() {
        return severity;
    }

    public void setSeverity(double severity) {
        this.severity = severity;
    }

    public List<String> getRelatedNoteIds() {
        return relatedNoteIds;
    }

    public void setRelatedNoteIds(List<String> relatedNoteIds) {
        this.relatedNoteIds = relatedNoteIds == null ? new ArrayList<>() : relatedNoteIds;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
