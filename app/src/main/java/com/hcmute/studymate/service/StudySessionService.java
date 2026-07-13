package com.hcmute.studymate.service;

import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.model.StudySession;
import com.hcmute.studymate.model.StudyStats;
import com.hcmute.studymate.repository.StudySessionRepository;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudySessionService {
    private static final int RECENT_LIMIT = 30;

    private final StudySessionRepository sessionRepository;

    public StudySessionService(StudySessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public void saveCompletedSession(String userId, Note note, long durationMillis, OperationCallback callback) {
        if (note == null || note.getId() == null) {
            callback.onError(new IllegalArgumentException("A saved note is required"));
            return;
        }
        if (durationMillis <= 0L) {
            callback.onError(new IllegalArgumentException("Duration must be greater than zero"));
            return;
        }

        StudySession session = new StudySession(
                null,
                userId,
                note.getId(),
                note.getTitle(),
                note.getCategory(),
                durationMillis,
                System.currentTimeMillis()
        );
        sessionRepository.saveSession(userId, session, callback);
    }

    public void loadRecentSessions(String userId, ListCallback<StudySession> callback) {
        sessionRepository.getRecentSessions(userId, RECENT_LIMIT, callback);
    }

    public void loadStats(String userId, DataCallback<StudyStats> callback) {
        sessionRepository.getRecentSessions(userId, RECENT_LIMIT, new ListCallback<StudySession>() {
            @Override
            public void onSuccess(List<StudySession> items) {
                callback.onSuccess(buildStats(items));
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }

    private StudyStats buildStats(List<StudySession> sessions) {
        long totalMillis = 0L;
        String latestNote = "";
        Map<String, Integer> categoryCounts = new HashMap<>();
        for (int index = 0; index < sessions.size(); index++) {
            StudySession session = sessions.get(index);
            totalMillis += session.getDurationMillis();
            if (index == 0 && session.getNoteTitle() != null) {
                latestNote = session.getNoteTitle();
            }
            String category = session.getCategory() == null || session.getCategory().trim().isEmpty()
                    ? "General"
                    : session.getCategory();
            categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
        }

        String topCategory = "";
        int topCount = 0;
        for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
            if (entry.getValue() > topCount) {
                topCount = entry.getValue();
                topCategory = entry.getKey();
            }
        }
        return new StudyStats(totalMillis, sessions.size(), topCategory, latestNote);
    }
}
