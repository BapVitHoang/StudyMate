package com.hcmute.studymate.model;

import java.util.ArrayList;
import java.util.List;

public class TutorReply {
    private String answer;
    private List<RagCitation> citations = new ArrayList<>();
    private List<String> suggestedFollowUps = new ArrayList<>();
    private String source;
    private boolean usedFallback;
    private long generatedAt;

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

    public List<String> getSuggestedFollowUps() {
        return suggestedFollowUps;
    }

    public void setSuggestedFollowUps(List<String> suggestedFollowUps) {
        this.suggestedFollowUps = suggestedFollowUps == null ? new ArrayList<>() : suggestedFollowUps;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isUsedFallback() {
        return usedFallback;
    }

    public void setUsedFallback(boolean usedFallback) {
        this.usedFallback = usedFallback;
    }

    public long getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(long generatedAt) {
        this.generatedAt = generatedAt;
    }
}
