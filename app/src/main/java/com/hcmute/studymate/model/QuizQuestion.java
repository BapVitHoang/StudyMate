package com.hcmute.studymate.model;

import java.util.ArrayList;
import java.util.List;

public class QuizQuestion {
    public static final String TYPE_MCQ = "mcq";
    public static final String TYPE_SHORT = "short";

    private String id;
    private String type;
    private String stem;
    private List<String> choices = new ArrayList<>();
    private String answer;
    private String explanation;
    private String sourceNoteId;
    private String sourceChunkId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStem() {
        return stem;
    }

    public void setStem(String stem) {
        this.stem = stem;
    }

    public List<String> getChoices() {
        return choices;
    }

    public void setChoices(List<String> choices) {
        this.choices = choices == null ? new ArrayList<>() : choices;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getSourceNoteId() {
        return sourceNoteId;
    }

    public void setSourceNoteId(String sourceNoteId) {
        this.sourceNoteId = sourceNoteId;
    }

    public String getSourceChunkId() {
        return sourceChunkId;
    }

    public void setSourceChunkId(String sourceChunkId) {
        this.sourceChunkId = sourceChunkId;
    }
}
