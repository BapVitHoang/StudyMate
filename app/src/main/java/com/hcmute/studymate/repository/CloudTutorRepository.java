package com.hcmute.studymate.repository;

import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.hcmute.studymate.model.RagCitation;
import com.hcmute.studymate.model.RetrievedChunk;
import com.hcmute.studymate.model.TutorMessage;
import com.hcmute.studymate.model.TutorReply;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.PassagePayloadMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudTutorRepository {
    private static final String FUNCTIONS_REGION = "asia-southeast1";
    private final FirebaseFunctions functions;

    public CloudTutorRepository() {
        this(FirebaseFunctions.getInstance(FUNCTIONS_REGION));
    }

    CloudTutorRepository(FirebaseFunctions functions) {
        this.functions = functions;
    }

    public void chat(List<TutorMessage> messages, List<RetrievedChunk> passages, String locale,
                     DataCallback<TutorReply> callback) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("locale", locale == null ? "en" : locale);
        payload.put("passages", PassagePayloadMapper.toPayload(passages));
        List<Map<String, Object>> messagePayload = new ArrayList<>();
        if (messages != null) {
            for (TutorMessage message : messages) {
                if (message == null) {
                    continue;
                }
                Map<String, Object> item = new HashMap<>();
                item.put("role", message.getRole());
                item.put("content", message.getContent());
                messagePayload.add(item);
            }
        }
        payload.put("messages", messagePayload);

        functions.getHttpsCallable(Constants.CALLABLE_TUTOR_CHAT)
                .call(payload)
                .addOnSuccessListener(result -> {
                    try {
                        callback.onSuccess(parseReply(result));
                    } catch (Exception exception) {
                        callback.onError(exception);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    @SuppressWarnings("unchecked")
    private TutorReply parseReply(HttpsCallableResult callableResult) {
        Object raw = callableResult.getData();
        if (!(raw instanceof Map)) {
            throw new IllegalStateException("Unexpected tutor response");
        }
        Map<String, Object> data = (Map<String, Object>) raw;
        TutorReply reply = new TutorReply();
        reply.setAnswer(asString(data.get("answer")));
        reply.setSource(asStringOrDefault(data.get("source"), Constants.RAG_SOURCE_CLOUD));
        reply.setGeneratedAt(asLong(data.get("generatedAt"), System.currentTimeMillis()));
        reply.setCitations(parseCitations(data.get("citations")));
        List<String> followUps = new ArrayList<>();
        Object followRaw = data.get("suggestedFollowUps");
        if (followRaw instanceof List) {
            for (Object item : (List<?>) followRaw) {
                if (item != null) {
                    followUps.add(String.valueOf(item));
                }
            }
        }
        reply.setSuggestedFollowUps(followUps);
        if (reply.getAnswer() == null || reply.getAnswer().trim().isEmpty()) {
            throw new IllegalStateException("Empty tutor answer");
        }
        return reply;
    }

    @SuppressWarnings("unchecked")
    private List<RagCitation> parseCitations(Object citationsRaw) {
        List<RagCitation> citations = new ArrayList<>();
        if (!(citationsRaw instanceof List)) {
            return citations;
        }
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
        return citations;
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
