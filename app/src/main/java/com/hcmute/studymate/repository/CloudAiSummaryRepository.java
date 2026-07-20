package com.hcmute.studymate.repository;

import com.hcmute.studymate.ai.GeminiApiClient;
import com.hcmute.studymate.ai.GeminiPromptBuilder;
import com.hcmute.studymate.model.SummarizeRequest;
import com.hcmute.studymate.model.SummaryResult;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.SummaryCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Gemini summarize via direct on-device API call (no Cloud Functions / Blaze).
 */
public class CloudAiSummaryRepository implements AiSummaryRepository {
    private final GeminiApiClient geminiApiClient;

    public CloudAiSummaryRepository() {
        this(GeminiApiClient.getInstance());
    }

    CloudAiSummaryRepository(GeminiApiClient geminiApiClient) {
        this.geminiApiClient = geminiApiClient;
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

        String locale = isBlank(request.getLocale()) ? "en" : request.getLocale();
        String prompt = GeminiPromptBuilder.summarize(
                locale, request.getTitle(), request.getCategory(), request.getContent());

        geminiApiClient.generateJsonAsync(prompt, 0.2, new GeminiApiClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                try {
                    callback.onSuccess(parseResult(request.getContent(), json));
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

    private SummaryResult parseResult(String originalContent, JSONObject data) throws Exception {
        String summaryText = data.optString("summaryText", "").trim();
        if (summaryText.isEmpty()) {
            throw new IllegalStateException("Gemini returned an empty summary");
        }
        SummaryResult result = new SummaryResult(
                originalContent,
                summaryText,
                Constants.SUMMARY_SOURCE_CLOUD,
                System.currentTimeMillis()
        );
        result.setBullets(asStringList(data.optJSONArray("bullets")));
        result.setKeyTerms(asStringList(data.optJSONArray("keyTerms")));
        double confidence = data.optDouble("confidence", 0.7);
        if (Double.isNaN(confidence)) {
            confidence = 0.7;
        }
        result.setConfidence(Math.max(0, Math.min(1, confidence)));
        result.setUsedFallback(false);
        return result;
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
