package com.hcmute.studymate.repository;

import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.hcmute.studymate.model.Concept;
import com.hcmute.studymate.model.ConceptEdge;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.DataCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudConceptRepository {
    private static final String FUNCTIONS_REGION = "asia-southeast1";
    private final FirebaseFunctions functions;

    public CloudConceptRepository() {
        this(FirebaseFunctions.getInstance(FUNCTIONS_REGION));
    }

    CloudConceptRepository(FirebaseFunctions functions) {
        this.functions = functions;
    }

    public void extract(String noteId, String title, String content, String locale,
                        DataCallback<ExtractResult> callback) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("noteId", noteId);
        payload.put("title", title);
        payload.put("content", content);
        payload.put("locale", locale == null ? "en" : locale);

        functions.getHttpsCallable(Constants.CALLABLE_EXTRACT_CONCEPTS)
                .call(payload)
                .addOnSuccessListener(result -> {
                    try {
                        callback.onSuccess(parse(result, noteId));
                    } catch (Exception exception) {
                        callback.onError(exception);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    @SuppressWarnings("unchecked")
    private ExtractResult parse(HttpsCallableResult callableResult, String noteId) {
        Object raw = callableResult.getData();
        if (!(raw instanceof Map)) {
            throw new IllegalStateException("Unexpected concept response");
        }
        Map<String, Object> data = (Map<String, Object>) raw;
        List<Concept> concepts = new ArrayList<>();
        Object conceptsRaw = data.get("concepts");
        if (conceptsRaw instanceof List) {
            for (Object item : (List<?>) conceptsRaw) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<String, Object> map = (Map<String, Object>) item;
                Concept concept = new Concept();
                concept.setName(asString(map.get("name")));
                concept.setDefinition(asString(map.get("definition")));
                Object importance = map.get("importance");
                concept.setImportance(importance instanceof Number
                        ? ((Number) importance).doubleValue() : 0.5);
                List<String> noteIds = new ArrayList<>();
                if (noteId != null) {
                    noteIds.add(noteId);
                }
                concept.setNoteIds(noteIds);
                concepts.add(concept);
            }
        }
        List<ConceptEdge> edges = new ArrayList<>();
        Object edgesRaw = data.get("edges");
        if (edgesRaw instanceof List) {
            for (Object item : (List<?>) edgesRaw) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<String, Object> map = (Map<String, Object>) item;
                ConceptEdge edge = new ConceptEdge();
                edge.setFromName(asString(map.get("from")));
                edge.setToName(asString(map.get("to")));
                edge.setRelation(asString(map.get("relation")));
                edge.setNoteId(noteId);
                edges.add(edge);
            }
        }
        return new ExtractResult(concepts, edges);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    public static final class ExtractResult {
        public final List<Concept> concepts;
        public final List<ConceptEdge> edges;

        public ExtractResult(List<Concept> concepts, List<ConceptEdge> edges) {
            this.concepts = concepts;
            this.edges = edges;
        }
    }
}
