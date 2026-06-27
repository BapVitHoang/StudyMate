package com.hcmute.studymate.model;

public class SummaryResult {
    private String originalText;
    private String summaryText;
    private String source;
    private long generatedAt;

    public SummaryResult() {
    }

    public SummaryResult(String originalText, String summaryText, String source, long generatedAt) {
        this.originalText = originalText;
        this.summaryText = summaryText;
        this.source = source;
        this.generatedAt = generatedAt;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
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
