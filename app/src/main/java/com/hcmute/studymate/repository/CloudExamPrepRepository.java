package com.hcmute.studymate.repository;

import com.hcmute.studymate.ai.GeminiApiClient;
import com.hcmute.studymate.ai.GeminiPromptBuilder;
import com.hcmute.studymate.model.ExamPrepResult;
import com.hcmute.studymate.model.ExamPrepSection;
import com.hcmute.studymate.model.RagCitation;
import com.hcmute.studymate.model.RetrievedChunk;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.DataCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CloudExamPrepRepository {
    private final GeminiApiClient geminiApiClient;

    public CloudExamPrepRepository() {
        this(GeminiApiClient.getInstance());
    }

    CloudExamPrepRepository(GeminiApiClient geminiApiClient) {
        this.geminiApiClient = geminiApiClient;
    }

    public void synthesize(String mode, String topic, List<RetrievedChunk> passages, String locale,
                           DataCallback<ExamPrepResult> callback) {
        if (passages == null || passages.isEmpty()) {
            callback.onError(new IllegalArgumentException("At least one passage is required"));
            return;
        }
        String safeMode = mode == null || mode.trim().isEmpty() ? "outline" : mode.trim();
        String safeLocale = locale == null ? "en" : locale;
        String prompt = GeminiPromptBuilder.examPrep(safeLocale, safeMode, topic, passages);
        geminiApiClient.generateJsonAsync(prompt, 0.25, new GeminiApiClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                try {
                    callback.onSuccess(parse(json, safeMode, topic, passages));
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

    private ExamPrepResult parse(JSONObject data, String mode, String topic,
                                 List<RetrievedChunk> passages) {
        ExamPrepResult result = new ExamPrepResult();
        result.setTitle(data.optString("title", "Exam prep"));
        result.setMode(mode);
        result.setTopic(topic);
        result.setSource(Constants.RAG_SOURCE_CLOUD);
        result.setGeneratedAt(System.currentTimeMillis());

        List<ExamPrepSection> sections = new ArrayList<>();
        JSONArray sectionsRaw = data.optJSONArray("sections");
        if (sectionsRaw != null) {
            for (int i = 0; i < sectionsRaw.length() && sections.size() < 8; i++) {
                JSONObject item = sectionsRaw.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String heading = item.optString("heading", "").trim();
                List<String> bullets = asStringList(item.optJSONArray("bullets"));
                if (heading.isEmpty() || bullets.isEmpty()) {
                    continue;
                }
                sections.add(new ExamPrepSection(heading, bullets));
            }
        }
        if (sections.isEmpty() && passages.get(0).getChunk() != null) {
            List<String> bullets = new ArrayList<>();
            bullets.add(GeminiApiClient.truncate(passages.get(0).getChunk().getText(), 240));
            sections.add(new ExamPrepSection("Key points", bullets));
        }
        result.setSections(sections);

        Set<String> allowed = allowedNoteIds(passages);
        List<RagCitation> citations = new ArrayList<>();
        JSONArray citationsRaw = data.optJSONArray("citations");
        if (citationsRaw != null) {
            for (int i = 0; i < citationsRaw.length() && citations.size() < 5; i++) {
                JSONObject item = citationsRaw.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String noteId = item.optString("noteId", "").trim();
                if (!allowed.contains(noteId)) {
                    continue;
                }
                citations.add(new RagCitation(
                        noteId,
                        item.optString("title", ""),
                        item.optString("excerpt", "")
                ));
            }
        }
        result.setCitations(citations);

        List<String> coverage = new ArrayList<>();
        JSONArray coverageRaw = data.optJSONArray("coverageNoteIds");
        if (coverageRaw != null) {
            for (int i = 0; i < coverageRaw.length(); i++) {
                String noteId = coverageRaw.optString(i, "").trim();
                if (allowed.contains(noteId) && !coverage.contains(noteId)) {
                    coverage.add(noteId);
                }
            }
        }
        if (coverage.isEmpty()) {
            coverage.addAll(allowed);
        }
        result.setCoverageNoteIds(coverage);
        return result;
    }

    private Set<String> allowedNoteIds(List<RetrievedChunk> passages) {
        Set<String> allowed = new HashSet<>();
        for (RetrievedChunk passage : passages) {
            if (passage.getChunk() != null && passage.getChunk().getNoteId() != null) {
                allowed.add(passage.getChunk().getNoteId());
            }
        }
        return allowed;
    }

    private List<String> asStringList(JSONArray array) {
        List<String> values = new ArrayList<>();
        if (array == null) {
            return values;
        }
        for (int i = 0; i < array.length(); i++) {
            String text = array.optString(i, "").trim();
            if (!text.isEmpty()) {
                values.add(text);
            }
        }
        return values;
    }
}
