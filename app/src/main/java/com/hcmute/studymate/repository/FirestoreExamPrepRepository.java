package com.hcmute.studymate.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hcmute.studymate.model.ExamPrepResult;
import com.hcmute.studymate.model.ExamPrepSection;
import com.hcmute.studymate.model.RagCitation;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreExamPrepRepository {
    private final FirebaseFirestore firestore;

    public FirestoreExamPrepRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreExamPrepRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public void save(String userId, ExamPrepResult result, OperationCallback callback) {
        CollectionReference ref = prepRef(userId);
        String id = result.getId() == null || result.getId().isEmpty()
                ? ref.document().getId() : result.getId();
        result.setId(id);
        ref.document(id).set(toMap(result))
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void list(String userId, ListCallback<ExamPrepResult> callback) {
        prepRef(userId).orderBy("generatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<ExamPrepResult> items = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        ExamPrepResult item = fromDocument(document);
                        if (item != null) {
                            items.add(item);
                        }
                    }
                    callback.onSuccess(items);
                })
                .addOnFailureListener(callback::onError);
    }

    public void get(String userId, String id, DataCallback<ExamPrepResult> callback) {
        prepRef(userId).document(id).get()
                .addOnSuccessListener(document -> {
                    ExamPrepResult item = fromDocument(document);
                    if (item == null) {
                        callback.onError(new IllegalStateException("Exam prep not found"));
                        return;
                    }
                    callback.onSuccess(item);
                })
                .addOnFailureListener(callback::onError);
    }

    private Map<String, Object> toMap(ExamPrepResult result) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", result.getTitle());
        data.put("mode", result.getMode());
        data.put("topic", result.getTopic());
        data.put("source", result.getSource());
        data.put("generatedAt", result.getGeneratedAt());
        data.put("coverageNoteIds", result.getCoverageNoteIds());
        List<Map<String, Object>> sections = new ArrayList<>();
        for (ExamPrepSection section : result.getSections()) {
            Map<String, Object> map = new HashMap<>();
            map.put("heading", section.getHeading());
            map.put("bullets", section.getBullets());
            sections.add(map);
        }
        data.put("sections", sections);
        List<Map<String, Object>> citations = new ArrayList<>();
        for (RagCitation citation : result.getCitations()) {
            Map<String, Object> map = new HashMap<>();
            map.put("noteId", citation.getNoteId());
            map.put("title", citation.getTitle());
            map.put("excerpt", citation.getExcerpt());
            citations.add(map);
        }
        data.put("citations", citations);
        return data;
    }

    @SuppressWarnings("unchecked")
    private ExamPrepResult fromDocument(DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return null;
        }
        ExamPrepResult result = new ExamPrepResult();
        result.setId(document.getId());
        result.setTitle(document.getString("title"));
        result.setMode(document.getString("mode"));
        result.setTopic(document.getString("topic"));
        result.setSource(document.getString("source"));
        Long generatedAt = document.getLong("generatedAt");
        result.setGeneratedAt(generatedAt == null ? 0L : generatedAt);
        Object coverage = document.get("coverageNoteIds");
        if (coverage instanceof List) {
            List<String> ids = new ArrayList<>();
            for (Object item : (List<?>) coverage) {
                if (item != null) {
                    ids.add(String.valueOf(item));
                }
            }
            result.setCoverageNoteIds(ids);
        }
        List<ExamPrepSection> sections = new ArrayList<>();
        Object sectionsRaw = document.get("sections");
        if (sectionsRaw instanceof List) {
            for (Object item : (List<?>) sectionsRaw) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<String, Object> map = (Map<String, Object>) item;
                List<String> bullets = new ArrayList<>();
                Object bulletsRaw = map.get("bullets");
                if (bulletsRaw instanceof List) {
                    for (Object bullet : (List<?>) bulletsRaw) {
                        if (bullet != null) {
                            bullets.add(String.valueOf(bullet));
                        }
                    }
                }
                sections.add(new ExamPrepSection(asString(map.get("heading")), bullets));
            }
        }
        result.setSections(sections);
        List<RagCitation> citations = new ArrayList<>();
        Object citationsRaw = document.get("citations");
        if (citationsRaw instanceof List) {
            for (Object item : (List<?>) citationsRaw) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<String, Object> map = (Map<String, Object>) item;
                citations.add(new RagCitation(
                        asString(map.get("noteId")),
                        asString(map.get("title")),
                        asString(map.get("excerpt"))
                ));
            }
        }
        result.setCitations(citations);
        return result;
    }

    private CollectionReference prepRef(String userId) {
        return firestore.collection("users").document(userId).collection("examPreps");
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
