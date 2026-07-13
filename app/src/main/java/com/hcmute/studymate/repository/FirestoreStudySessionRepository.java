package com.hcmute.studymate.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.hcmute.studymate.model.StudySession;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.ArrayList;
import java.util.List;

public class FirestoreStudySessionRepository implements StudySessionRepository {
    private static final String USERS_COLLECTION = "users";
    private static final String SESSIONS_COLLECTION = "studySessions";

    private final FirebaseFirestore firestore;

    public FirestoreStudySessionRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreStudySessionRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void saveSession(String userId, StudySession session, OperationCallback callback) {
        if (isBlank(userId) || session == null) {
            callback.onError(new IllegalArgumentException("User id and session are required"));
            return;
        }

        CollectionReference sessionsRef = sessionsRef(userId);
        if (isBlank(session.getId())) {
            session.setId(sessionsRef.document().getId());
        }
        session.setUserId(userId);

        sessionsRef.document(session.getId())
                .set(session)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void getRecentSessions(String userId, int limit, ListCallback<StudySession> callback) {
        if (isBlank(userId)) {
            callback.onError(new IllegalArgumentException("User id is required"));
            return;
        }

        Query query = sessionsRef(userId).orderBy("completedAt", Query.Direction.DESCENDING);
        if (limit > 0) {
            query = query.limit(limit);
        }

        query.get()
                .addOnSuccessListener(snapshot -> {
                    List<StudySession> sessions = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        StudySession session = document.toObject(StudySession.class);
                        if (session != null) {
                            if (isBlank(session.getId())) {
                                session.setId(document.getId());
                            }
                            if (isBlank(session.getUserId())) {
                                session.setUserId(userId);
                            }
                            sessions.add(session);
                        }
                    }
                    callback.onSuccess(sessions);
                })
                .addOnFailureListener(callback::onError);
    }

    private CollectionReference sessionsRef(String userId) {
        return firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(SESSIONS_COLLECTION);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
