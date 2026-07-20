package com.hcmute.studymate.repository;

import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.hcmute.studymate.model.SummarizeRequest;
import com.hcmute.studymate.model.SummaryResult;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.SummaryCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudAiSummaryRepository implements AiSummaryRepository {
    private static final String FUNCTIONS_REGION = "asia-southeast1";

    private final FirebaseFunctions functions;

    public CloudAiSummaryRepository() {
        this(FirebaseFunctions.getInstance(FUNCTIONS_REGION));
    }

    CloudAiSummaryRepository(FirebaseFunctions functions) {
        this.functions = functions;
    }

    @Override
    public void summarize(SummarizeRequest request, SummaryCallback callback) {
        if (request == null) {
            callback.onError(new IllegalArgumentException("Summarize request is required"));
            return;
        }
        if (isBlank(request.getNoteId()) && isBlank(request.getContent())) {
            callback.onError(new IllegalArgumentException("Note id or content is required"));
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        putIfPresent(payload, "noteId", request.getNoteId());
        putIfPresent(payload, "title", request.getTitle());
        putIfPresent(payload, "content", request.getContent());
        putIfPresent(payload, "category", request.getCategory());
        putIfPresent(payload, "locale", request.getLocale());
        payload.put("task", "summarize");

        functions.getHttpsCallable(Constants.CALLABLE_SUMMARIZE_NOTE)
                .call(payload)
                .addOnSuccessListener(result -> {
                    try {
                        callback.onSuccess(parseResult(request.getContent(), result));
                    } catch (Exception exception) {
                        callback.onError(exception);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    @SuppressWarnings("unchecked")
    private SummaryResult parseResult(String originalContent, HttpsCallableResult callableResult) {
        Object rawData = callableResult.getData();
        if (!(rawData instanceof Map)) {
            throw new IllegalStateException("Cloud summarize returned an unexpected payload");
        }

        Map<String, Object> data = (Map<String, Object>) rawData;
        String summaryText = asString(data.get("summaryText"));
        if (isBlank(summaryText)) {
            throw new IllegalStateException("Cloud summarize returned an empty summary");
        }

        SummaryResult result = new SummaryResult(
                originalContent,
                summaryText,
                asStringOrDefault(data.get("source"), Constants.SUMMARY_SOURCE_CLOUD),
                asLongOrDefault(data.get("generatedAt"), System.currentTimeMillis())
        );
        result.setBullets(asStringList(data.get("bullets")));
        result.setKeyTerms(asStringList(data.get("keyTerms")));
        result.setConfidence(asDouble(data.get("confidence")));
        result.setUsedFallback(false);
        return result;
    }

    private void putIfPresent(Map<String, Object> payload, String key, String value) {
        if (!isBlank(value)) {
            payload.put(key, value);
        }
    }

    private List<String> asStringList(Object value) {
        List<String> values = new ArrayList<>();
        if (!(value instanceof List)) {
            return values;
        }
        for (Object item : (List<?>) value) {
            if (item != null) {
                String text = String.valueOf(item).trim();
                if (!text.isEmpty()) {
                    values.add(text);
                }
            }
        }
        return values;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private String asStringOrDefault(Object value, String fallback) {
        String text = asString(value);
        return isBlank(text) ? fallback : text;
    }

    private Double asDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private long asLongOrDefault(Object value, long fallback) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return fallback;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
