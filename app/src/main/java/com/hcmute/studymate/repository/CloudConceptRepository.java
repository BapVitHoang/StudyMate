package com.hcmute.studymate.repository;

import com.hcmute.studymate.ai.GeminiApiClient;
import com.hcmute.studymate.ai.GeminiPromptBuilder;
import com.hcmute.studymate.model.Concept;
import com.hcmute.studymate.model.ConceptEdge;
import com.hcmute.studymate.utils.DataCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CloudConceptRepository {
    private final GeminiApiClient geminiApiClient;

    public CloudConceptRepository() {
        this(GeminiApiClient.getInstance());
    }

    CloudConceptRepository(GeminiApiClient geminiApiClient) {
        this.geminiApiClient = geminiApiClient;
    }

    public void extract(String noteId, String title, String content, String locale,
                        DataCallback<ExtractResult> callback) {
        if (content == null || content.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Note content is required"));
            return;
        }
        String safeLocale = locale == null ? "en" : locale;
        String prompt = GeminiPromptBuilder.concepts(safeLocale, noteId, title, content);
        geminiApiClient.generateJsonAsync(prompt, 0.2, new GeminiApiClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                try {
                    callback.onSuccess(parse(json, noteId));
                } catch (Exception exception) {
                    callback.onError(exception);
                }
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }

    private ExtractResult parse(JSONObject data, String noteId) throws Exception {
        List<Concept> concepts = new ArrayList<>();
        Set<String> nameSet = new HashSet<>();
        JSONArray conceptsRaw = data.optJSONArray("concepts");
        if (conceptsRaw != null) {
            for (int i = 0; i < conceptsRaw.length() && concepts.size() < 12; i++) {
                JSONObject item = conceptsRaw.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String name = item.optString("name", "").trim();
                if (name.isEmpty() || !nameSet.add(name.toLowerCase(Locale.US))) {
                    continue;
                }
                Concept concept = new Concept();
                concept.setName(name);
                concept.setDefinition(item.optString("definition", ""));
                double importance = item.optDouble("importance", 0.5);
                if (Double.isNaN(importance)) {
                    importance = 0.5;
                }
                concept.setImportance(Math.max(0, Math.min(1, importance)));
                List<String> noteIds = new ArrayList<>();
                if (noteId != null && !noteId.trim().isEmpty()) {
                    noteIds.add(noteId);
                }
                concept.setNoteIds(noteIds);
                concepts.add(concept);
            }
        }

        List<ConceptEdge> edges = new ArrayList<>();
        JSONArray edgesRaw = data.optJSONArray("edges");
        if (edgesRaw != null) {
            for (int i = 0; i < edgesRaw.length() && edges.size() < 20; i++) {
                JSONObject item = edgesRaw.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String from = item.optString("from", "").trim();
                String to = item.optString("to", "").trim();
                if (from.isEmpty() || to.isEmpty() || from.equalsIgnoreCase(to)) {
                    continue;
                }
                ConceptEdge edge = new ConceptEdge();
                edge.setFromName(from);
                edge.setToName(to);
                String relation = item.optString("relation", "relatedTo").trim();
                edge.setRelation("prerequisiteOf".equals(relation) ? "prerequisiteOf" : "relatedTo");
                edge.setNoteId(noteId);
                edges.add(edge);
            }
        }
        if (concepts.isEmpty()) {
            throw new IllegalStateException("Gemini returned no concepts");
        }
        return new ExtractResult(concepts, edges);
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
