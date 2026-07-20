package com.hcmute.studymate.model;

import java.util.ArrayList;
import java.util.List;

public class ExamPrepResult {
    private String id;
    private String title;
    private String mode;
    private String topic;
    private List<ExamPrepSection> sections = new ArrayList<>();
    private List<RagCitation> citations = new ArrayList<>();
    private List<String> coverageNoteIds = new ArrayList<>();
    private String source;
    private long generatedAt;

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

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public List<ExamPrepSection> getSections() {
        return sections;
    }

    public void setSections(List<ExamPrepSection> sections) {
        this.sections = sections == null ? new ArrayList<>() : sections;
    }

    public List<RagCitation> getCitations() {
        return citations;
    }

    public void setCitations(List<RagCitation> citations) {
        this.citations = citations == null ? new ArrayList<>() : citations;
    }

    public List<String> getCoverageNoteIds() {
        return coverageNoteIds;
    }

    public void setCoverageNoteIds(List<String> coverageNoteIds) {
        this.coverageNoteIds = coverageNoteIds == null ? new ArrayList<>() : coverageNoteIds;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(long generatedAt) {
        this.generatedAt = generatedAt;
    }
}
