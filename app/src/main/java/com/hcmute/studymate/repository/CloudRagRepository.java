package com.hcmute.studymate.repository;

import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.hcmute.studymate.model.RagAnswer;
import com.hcmute.studymate.model.RagCitation;
import com.hcmute.studymate.model.RetrievedChunk;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.DataCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudRagRepository {
    private static final String FUNCTIONS_REGION = "asia-southeast1";

    private final FirebaseFunctions functions;

    public CloudRagRepository() {
        this(FirebaseFunctions.getInstance(FUNCTIONS_REGION));
    }

    CloudRagRepository(FirebaseFunctions functions) {
        this.functions = functions;
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

        List<Map<String, Object>> payloadPassages = new ArrayList<>();
        for (RetrievedChunk passage : passages) {
            if (passage.getChunk() == null) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("noteId", passage.getChunk().getNoteId());
            item.put("chunkId", passage.getChunk().getId());
            item.put("title", passage.getChunk().getTitle());
            item.put("text", passage.getChunk().getText());
            payloadPassages.add(item);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("question", question.trim());
        payload.put("locale", locale == null ? "en" : locale);
        payload.put("passages", payloadPassages);

        functions.getHttpsCallable(Constants.CALLABLE_ANSWER_FROM_NOTES)
                .call(payload)
                .addOnSuccessListener(result -> {
                    try {
                        callback.onSuccess(parseAnswer(result));
                    } catch (Exception exception) {
                        callback.onError(exception);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    @SuppressWarnings("unchecked")
    private RagAnswer parseAnswer(HttpsCallableResult callableResult) {
        Object raw = callableResult.getData();
        if (!(raw instanceof Map)) {
            throw new IllegalStateException("Unexpected RAG response payload");
        }
        Map<String, Object> data = (Map<String, Object>) raw;
        String answer = asString(data.get("answer"));
        if (answer == null || answer.trim().isEmpty()) {
            throw new IllegalStateException("Empty RAG answer");
        }

        List<RagCitation> citations = new ArrayList<>();
        Object citationsRaw = data.get("citations");
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

        long generatedAt = System.currentTimeMillis();
        Object generatedRaw = data.get("generatedAt");
        if (generatedRaw instanceof Number) {
            generatedAt = ((Number) generatedRaw).longValue();
        }

        return new RagAnswer(answer, citations,
                asStringOrDefault(data.get("source"), Constants.SUMMARY_SOURCE_CLOUD),
                generatedAt);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private String asStringOrDefault(Object value, String fallback) {
        String text = asString(value);
        return text == null || text.isEmpty() ? fallback : text;
    }
}
