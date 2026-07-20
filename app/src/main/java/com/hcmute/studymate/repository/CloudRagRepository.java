package com.hcmute.studymate.repository;

import com.hcmute.studymate.ai.GeminiApiClient;
import com.hcmute.studymate.ai.GeminiPromptBuilder;
import com.hcmute.studymate.model.RagAnswer;
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

public class CloudRagRepository {
    private final GeminiApiClient geminiApiClient;

    public CloudRagRepository() {
        this(GeminiApiClient.getInstance());
    }

    CloudRagRepository(GeminiApiClient geminiApiClient) {
        this.geminiApiClient = geminiApiClient;
    }

    public void answerFromNotes(String question, List<RetrievedChunk> passages, String locale,
                                DataCallback<RagAnswer> callback) {
        if (question == null || question.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Question is required"));
            return;
        }
        if (passages == null || passages.isEmpty()) {
            callback.onError(new IllegalArgumentException("No retrieved passages available"));
            return;
        }

        String safeLocale = locale == null ? "en" : locale;
        String prompt = GeminiPromptBuilder.groundedAnswer(safeLocale, question.trim(), passages);
        geminiApiClient.generateJsonAsync(prompt, 0.2, new GeminiApiClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                try {
                    callback.onSuccess(parseAnswer(json, passages));
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

    private RagAnswer parseAnswer(JSONObject data, List<RetrievedChunk> passages) throws Exception {
        String answer = data.optString("answer", "").trim();
        if (answer.isEmpty()) {
            throw new IllegalStateException("Empty RAG answer");
        }

        Set<String> allowed = new HashSet<>();
        for (RetrievedChunk passage : passages) {
            if (passage.getChunk() != null && passage.getChunk().getNoteId() != null) {
                allowed.add(passage.getChunk().getNoteId());
            }
        }

        List<RagCitation> citations = new ArrayList<>();
        JSONArray citationsRaw = data.optJSONArray("citations");
        if (citationsRaw != null) {
            for (int i = 0; i < citationsRaw.length(); i++) {
                JSONObject item = citationsRaw.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String noteId = item.optString("noteId", "").trim();
                if (noteId.isEmpty() || !allowed.contains(noteId)) {
                    continue;
                }
                citations.add(new RagCitation(
                        noteId,
                        item.optString("title", ""),
                        item.optString("excerpt", "")
                ));
                if (citations.size() >= 5) {
                    break;
                }
            }
        }
        if (citations.isEmpty() && passages.get(0).getChunk() != null) {
            citations.add(new RagCitation(
                    passages.get(0).getChunk().getNoteId(),
                    passages.get(0).getChunk().getTitle(),
                    GeminiApiClient.truncate(passages.get(0).getChunk().getText(), 180)
            ));
        }

        return new RagAnswer(answer, citations, Constants.RAG_SOURCE_CLOUD, System.currentTimeMillis());
    }
}
