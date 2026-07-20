package com.hcmute.studymate.model;

import java.util.ArrayList;
import java.util.List;

public class RagAnswer {
    private String answer;
    private List<RagCitation> citations = new ArrayList<>();
    private String source;
    private long generatedAt;
    private boolean usedFallback;

    public RagAnswer() {
    }

    public RagAnswer(String answer, List<RagCitation> citations, String source, long generatedAt) {
        this.answer = answer;
        this.citations = citations == null ? new ArrayList<>() : citations;
        this.source = source;
        this.generatedAt = generatedAt;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<RagCitation> getCitations() {
        return citations;
    }

    public void setCitations(List<RagCitation> citations) {
        this.citations = citations == null ? new ArrayList<>() : citations;
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

    public boolean isUsedFallback() {
        return usedFallback;
    }

    public void setUsedFallback(boolean usedFallback) {
        this.usedFallback = usedFallback;
    }
}
