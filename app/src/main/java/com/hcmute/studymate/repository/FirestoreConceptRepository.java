package com.hcmute.studymate.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.hcmute.studymate.model.Concept;
import com.hcmute.studymate.model.ConceptEdge;
import com.hcmute.studymate.model.LearningGap;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirestoreConceptRepository {
    private final FirebaseFirestore firestore;

    public FirestoreConceptRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreConceptRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public void upsertConceptsAndEdges(String userId, List<Concept> concepts, List<ConceptEdge> edges,
                                       OperationCallback callback) {
        WriteBatch batch = firestore.batch();
        CollectionReference conceptsRef = conceptsRef(userId);
        CollectionReference edgesRef = edgesRef(userId);
        if (concepts != null) {
            for (Concept concept : concepts) {
                if (concept == null || concept.getName() == null || concept.getName().trim().isEmpty()) {
                    continue;
                }
                String id = slug(concept.getName());
                concept.setId(id);
                batch.set(conceptsRef.document(id), conceptToMap(concept),
                        com.google.firebase.firestore.SetOptions.merge());
            }
        }
        if (edges != null) {
            for (ConceptEdge edge : edges) {
                if (edge == null || edge.getFromName() == null || edge.getToName() == null) {
                    continue;
                }
                String id = slug(edge.getFromName() + "_" + edge.getRelation() + "_" + edge.getToName());
                edge.setId(id);
                batch.set(edgesRef.document(id), edgeToMap(edge),
                        com.google.firebase.firestore.SetOptions.merge());
            }
        }
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void listConcepts(String userId, ListCallback<Concept> callback) {
        conceptsRef(userId).get()
                .addOnSuccessListener(snapshot -> {
                    List<Concept> concepts = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Concept concept = conceptFromDocument(document);
                        if (concept != null) {
                            concepts.add(concept);
                        }
                    }
                    callback.onSuccess(concepts);
                })
                .addOnFailureListener(callback::onError);
    }

    public void listEdges(String userId, ListCallback<ConceptEdge> callback) {
        edgesRef(userId).get()
                .addOnSuccessListener(snapshot -> {
                    List<ConceptEdge> edges = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        ConceptEdge edge = edgeFromDocument(document);
                        if (edge != null) {
                            edges.add(edge);
                        }
                    }
                    callback.onSuccess(edges);
                })
                .addOnFailureListener(callback::onError);
    }

    public void replaceGaps(String userId, List<LearningGap> gaps, OperationCallback callback) {
        gapsRef(userId).get()
                .addOnSuccessListener(snapshot -> {
                    WriteBatch batch = firestore.batch();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        batch.delete(document.getReference());
                    }
                    if (gaps != null) {
                        for (LearningGap gap : gaps) {
                            String id = gap.getId() == null || gap.getId().isEmpty()
                                    ? slug(gap.getConceptName()) : gap.getId();
                            gap.setId(id);
                            batch.set(gapsRef(userId).document(id), gapToMap(gap));
                        }
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void listGaps(String userId, ListCallback<LearningGap> callback) {
        gapsRef(userId).get()
                .addOnSuccessListener(snapshot -> {
                    List<LearningGap> gaps = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        LearningGap gap = gapFromDocument(document);
                        if (gap != null) {
                            gaps.add(gap);
                        }
                    }
                    callback.onSuccess(gaps);
                })
                .addOnFailureListener(callback::onError);
    }

    private Map<String, Object> conceptToMap(Concept concept) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", concept.getName());
        data.put("definition", concept.getDefinition());
        data.put("importance", concept.getImportance());
        data.put("noteIds", concept.getNoteIds());
        data.put("embedding", concept.getEmbedding());
        data.put("updatedAt", concept.getUpdatedAt());
        return data;
    }

    private Map<String, Object> edgeToMap(ConceptEdge edge) {
        Map<String, Object> data = new HashMap<>();
        data.put("fromName", edge.getFromName());
        data.put("toName", edge.getToName());
        data.put("relation", edge.getRelation());
        data.put("noteId", edge.getNoteId());
        return data;
    }

    private Map<String, Object> gapToMap(LearningGap gap) {
        Map<String, Object> data = new HashMap<>();
        data.put("conceptName", gap.getConceptName());
        data.put("reason", gap.getReason());
        data.put("severity", gap.getSeverity());
        data.put("relatedNoteIds", gap.getRelatedNoteIds());
        data.put("updatedAt", gap.getUpdatedAt());
        return data;
    }

    private Concept conceptFromDocument(DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return null;
        }
        Concept concept = new Concept();
        concept.setId(document.getId());
        concept.setName(document.getString("name"));
        concept.setDefinition(document.getString("definition"));
        Double importance = document.getDouble("importance");
        concept.setImportance(importance == null ? 0.5 : importance);
        Long updatedAt = document.getLong("updatedAt");
        concept.setUpdatedAt(updatedAt == null ? 0L : updatedAt);
        Object noteIds = document.get("noteIds");
        if (noteIds instanceof List) {
            List<String> ids = new ArrayList<>();
            for (Object item : (List<?>) noteIds) {
                if (item != null) {
                    ids.add(String.valueOf(item));
                }
            }
            concept.setNoteIds(ids);
        }
        Object embedding = document.get("embedding");
        if (embedding instanceof List) {
            List<Double> values = new ArrayList<>();
            for (Object item : (List<?>) embedding) {
                if (item instanceof Number) {
                    values.add(((Number) item).doubleValue());
                }
            }
            concept.setEmbedding(values);
        }
        return concept;
    }

    private ConceptEdge edgeFromDocument(DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return null;
        }
        ConceptEdge edge = new ConceptEdge();
        edge.setId(document.getId());
        edge.setFromName(document.getString("fromName"));
        edge.setToName(document.getString("toName"));
        edge.setRelation(document.getString("relation"));
        edge.setNoteId(document.getString("noteId"));
        return edge;
    }

    private LearningGap gapFromDocument(DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return null;
        }
        LearningGap gap = new LearningGap();
        gap.setId(document.getId());
        gap.setConceptName(document.getString("conceptName"));
        gap.setReason(document.getString("reason"));
        Double severity = document.getDouble("severity");
        gap.setSeverity(severity == null ? 0.5 : severity);
        Long updatedAt = document.getLong("updatedAt");
        gap.setUpdatedAt(updatedAt == null ? 0L : updatedAt);
        Object related = document.get("relatedNoteIds");
        if (related instanceof List) {
            List<String> ids = new ArrayList<>();
            for (Object item : (List<?>) related) {
                if (item != null) {
                    ids.add(String.valueOf(item));
                }
            }
            gap.setRelatedNoteIds(ids);
        }
        return gap;
    }

    private CollectionReference conceptsRef(String userId) {
        return firestore.collection("users").document(userId).collection("concepts");
    }

    private CollectionReference edgesRef(String userId) {
        return firestore.collection("users").document(userId).collection("conceptEdges");
    }

    private CollectionReference gapsRef(String userId) {
        return firestore.collection("users").document(userId).collection("learningGaps");
    }

    private String slug(String value) {
        String normalized = value == null ? "item" : value.trim().toLowerCase(Locale.US);
        normalized = normalized.replaceAll("[^a-z0-9]+", "_");
        if (normalized.isEmpty()) {
            return "item";
        }
        if (normalized.length() > 80) {
            return normalized.substring(0, 80);
        }
        return normalized;
    }
}
