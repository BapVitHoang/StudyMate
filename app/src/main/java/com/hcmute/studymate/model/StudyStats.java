package com.hcmute.studymate.model;

public class StudyStats {
    private final long totalMillis;
    private final int sessionCount;
    private final String topCategory;
    private final String latestNoteTitle;

    public StudyStats(long totalMillis, int sessionCount, String topCategory, String latestNoteTitle) {
        this.totalMillis = totalMillis;
        this.sessionCount = sessionCount;
        this.topCategory = topCategory;
        this.latestNoteTitle = latestNoteTitle;
    }

    public long getTotalMillis() {
        return totalMillis;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public String getTopCategory() {
        return topCategory;
    }

    public String getLatestNoteTitle() {
        return latestNoteTitle;
    }
}
