package com.hcmute.studymate.model;

import java.util.ArrayList;
import java.util.List;

public class SummaryResult {
    private String originalText;
    private String summaryText;
    private List<String> bullets = new ArrayList<>();
    private List<String> keyTerms = new ArrayList<>();
    private Double confidence;
    private String source;
    private long generatedAt;
    private boolean usedFallback;
    private String cloudFailureReason;

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

    public List<String> getBullets() {
        return bullets;
    }

    public void setBullets(List<String> bullets) {
        this.bullets = bullets == null ? new ArrayList<>() : bullets;
    }

    public List<String> getKeyTerms() {
        return keyTerms;
    }

    public void setKeyTerms(List<String> keyTerms) {
        this.keyTerms = keyTerms == null ? new ArrayList<>() : keyTerms;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
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

    public String getCloudFailureReason() {
        return cloudFailureReason;
    }

    public void setCloudFailureReason(String cloudFailureReason) {
        this.cloudFailureReason = cloudFailureReason;
    }
}
