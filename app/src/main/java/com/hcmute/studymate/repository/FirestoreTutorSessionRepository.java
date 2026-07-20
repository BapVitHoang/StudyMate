package com.hcmute.studymate.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hcmute.studymate.model.TutorMessage;
import com.hcmute.studymate.model.TutorSession;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreTutorSessionRepository {
    private final FirebaseFirestore firestore;

    public FirestoreTutorSessionRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreTutorSessionRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public void saveSession(String userId, TutorSession session, OperationCallback callback) {
        if (isBlank(userId) || session == null) {
            callback.onError(new IllegalArgumentException("User and session required"));
            return;
        }
        CollectionReference ref = sessionsRef(userId);
        String id = isBlank(session.getId()) ? ref.document().getId() : session.getId();
        session.setId(id);
        session.setUpdatedAt(System.currentTimeMillis());
        ref.document(id).set(toMap(session))
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void listSessions(String userId, ListCallback<TutorSession> callback) {
        if (isBlank(userId)) {
            callback.onError(new IllegalArgumentException("User id required"));
            return;
        }
        sessionsRef(userId).orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<TutorSession> sessions = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        TutorSession session = fromDocument(document);
                        if (session != null) {
                            sessions.add(session);
                        }
                    }
                    callback.onSuccess(sessions);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getSession(String userId, String sessionId, DataCallback<TutorSession> callback) {
        sessionsRef(userId).document(sessionId).get()
                .addOnSuccessListener(document -> {
                    TutorSession session = fromDocument(document);
                    if (session == null) {
                        callback.onError(new IllegalStateException("Session not found"));
                        return;
                    }
                    callback.onSuccess(session);
                })
                .addOnFailureListener(callback::onError);
    }

    private Map<String, Object> toMap(TutorSession session) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", session.getTitle());
        data.put("updatedAt", session.getUpdatedAt());
        List<Map<String, Object>> messages = new ArrayList<>();
        if (session.getMessages() != null) {
            for (TutorMessage message : session.getMessages()) {
                Map<String, Object> item = new HashMap<>();
                item.put("role", message.getRole());
                item.put("content", message.getContent());
                item.put("createdAt", message.getCreatedAt());
                messages.add(item);
            }
        }
        data.put("messages", messages);
        return data;
    }

    @SuppressWarnings("unchecked")
    private TutorSession fromDocument(DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return null;
        }
        TutorSession session = new TutorSession();
        session.setId(document.getId());
        session.setTitle(document.getString("title"));
        Long updatedAt = document.getLong("updatedAt");
        session.setUpdatedAt(updatedAt == null ? 0L : updatedAt);
        List<TutorMessage> messages = new ArrayList<>();
        Object raw = document.get("messages");
        if (raw instanceof List) {
            for (Object item : (List<?>) raw) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<String, Object> map = (Map<String, Object>) item;
                TutorMessage message = new TutorMessage();
                message.setRole(asString(map.get("role")));
                message.setContent(asString(map.get("content")));
                Object created = map.get("createdAt");
                if (created instanceof Number) {
                    message.setCreatedAt(((Number) created).longValue());
                }
                messages.add(message);
            }
        }
        session.setMessages(messages);
        return session;
    }

    private CollectionReference sessionsRef(String userId) {
        return firestore.collection("users").document(userId).collection("tutorSessions");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
