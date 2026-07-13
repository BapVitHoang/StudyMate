package com.hcmute.studymate.controller;

import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.model.StudySession;
import com.hcmute.studymate.model.StudyStats;
import com.hcmute.studymate.service.StudySessionService;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

public class StudySessionController {
    private final StudySessionService sessionService;

    public StudySessionController(StudySessionService sessionService) {
        this.sessionService = sessionService;
    }

    public void saveCompletedSession(String userId, Note note, long durationMillis, OperationCallback callback) {
        sessionService.saveCompletedSession(userId, note, durationMillis, callback);
    }

    public void loadRecentSessions(String userId, ListCallback<StudySession> callback) {
        sessionService.loadRecentSessions(userId, callback);
    }

    public void loadStats(String userId, DataCallback<StudyStats> callback) {
        sessionService.loadStats(userId, callback);
    }
}
