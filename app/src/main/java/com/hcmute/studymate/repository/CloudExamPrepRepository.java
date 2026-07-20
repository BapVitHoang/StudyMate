package com.hcmute.studymate.repository;

import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.hcmute.studymate.model.ExamPrepResult;
import com.hcmute.studymate.model.ExamPrepSection;
import com.hcmute.studymate.model.RagCitation;
import com.hcmute.studymate.model.RetrievedChunk;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.PassagePayloadMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudExamPrepRepository {
    private static final String FUNCTIONS_REGION = "asia-southeast1";
    private final FirebaseFunctions functions;

    public CloudExamPrepRepository() {
        this(FirebaseFunctions.getInstance(FUNCTIONS_REGION));
    }

    CloudExamPrepRepository(FirebaseFunctions functions) {
        this.functions = functions;
    }

    public void synthesize(String mode, String topic, List<RetrievedChunk> passages, String locale,
                           DataCallback<ExamPrepResult> callback) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("mode", mode);
        payload.put("topic", topic == null ? "" : topic);
        payload.put("locale", locale == null ? "en" : locale);
        payload.put("passages", PassagePayloadMapper.toPayload(passages));

        functions.getHttpsCallable(Constants.CALLABLE_SYNTHESIZE_EXAM_PREP)
                .call(payload)
                .addOnSuccessListener(result -> {
                    try {
                        callback.onSuccess(parse(result, mode, topic));
                    } catch (Exception exception) {
                        callback.onError(exception);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    @SuppressWarnings("unchecked")
    private ExamPrepResult parse(HttpsCallableResult callableResult, String mode, String topic) {
        Object raw = callableResult.getData();
        if (!(raw instanceof Map)) {
            throw new IllegalStateException("Unexpected exam prep response");
        }
        Map<String, Object> data = (Map<String, Object>) raw;
        ExamPrepResult result = new ExamPrepResult();
        result.setTitle(asString(data.get("title")));
        result.setMode(asStringOrDefault(data.get("mode"), mode));
        result.setTopic(topic);
        result.setSource(asStringOrDefault(data.get("source"), Constants.RAG_SOURCE_CLOUD));
        result.setGeneratedAt(asLong(data.get("generatedAt"), System.currentTimeMillis()));

        List<ExamPrepSection> sections = new ArrayList<>();
        Object sectionsRaw = data.get("sections");
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
        result.setCitations(citations);

        List<String> coverage = new ArrayList<>();
        Object coverageRaw = data.get("coverageNoteIds");
        if (coverageRaw instanceof List) {
            for (Object item : (List<?>) coverageRaw) {
                if (item != null) {
                    coverage.add(String.valueOf(item));
                }
            }
        }
        result.setCoverageNoteIds(coverage);
        return result;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private String asStringOrDefault(Object value, String fallback) {
        String text = asString(value);
        return text == null || text.isEmpty() ? fallback : text;
    }

    private long asLong(Object value, long fallback) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return fallback;
    }
}
