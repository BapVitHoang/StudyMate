package com.hcmute.studymate.repository;

import com.hcmute.studymate.model.StudySession;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

public interface StudySessionRepository {
    void saveSession(String userId, StudySession session, OperationCallback callback);

    void getRecentSessions(String userId, int limit, ListCallback<StudySession> callback);
}
