package com.hcmute.studymate.repository;

import com.hcmute.studymate.ai.GeminiApiClient;
import com.hcmute.studymate.ai.GeminiPromptBuilder;
import com.hcmute.studymate.model.RagCitation;
import com.hcmute.studymate.model.RetrievedChunk;
import com.hcmute.studymate.model.TutorMessage;
import com.hcmute.studymate.model.TutorReply;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.DataCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CloudTutorRepository {
    private final GeminiApiClient geminiApiClient;

    public CloudTutorRepository() {
        this(GeminiApiClient.getInstance());
    }

    CloudTutorRepository(GeminiApiClient geminiApiClient) {
        this.geminiApiClient = geminiApiClient;
    }

    public void chat(List<TutorMessage> messages, List<RetrievedChunk> passages, String locale,
                     DataCallback<TutorReply> callback) {
        if (messages == null || messages.isEmpty()) {
            callback.onError(new IllegalArgumentException("At least one chat message is required"));
            return;
        }
        if (passages == null || passages.isEmpty()) {
            callback.onError(new IllegalArgumentException("At least one passage is required"));
            return;
        }

        StringBuilder history = new StringBuilder();
        for (TutorMessage message : messages) {
            if (message == null || message.getContent() == null) {
                continue;
            }
            history.append(String.valueOf(message.getRole()).toUpperCase())
                    .append(": ")
                    .append(message.getContent())
                    .append('\n');
        }

        String safeLocale = locale == null ? "en" : locale;
        String prompt = GeminiPromptBuilder.tutor(safeLocale, history.toString().trim(), passages);
        geminiApiClient.generateJsonAsync(prompt, 0.3, new GeminiApiClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                try {
                    callback.onSuccess(parseReply(json, passages));
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

    private TutorReply parseReply(JSONObject data, List<RetrievedChunk> passages) throws Exception {
        String answer = data.optString("answer", "").trim();
        if (answer.isEmpty()) {
            throw new IllegalStateException("Empty tutor answer");
        }

        TutorReply reply = new TutorReply();
        reply.setAnswer(answer);
        reply.setSource(Constants.RAG_SOURCE_CLOUD);
        reply.setGeneratedAt(System.currentTimeMillis());
        reply.setCitations(parseCitations(data.optJSONArray("citations"), passages));

        List<String> followUps = new ArrayList<>();
        JSONArray followRaw = data.optJSONArray("suggestedFollowUps");
        if (followRaw != null) {
            for (int i = 0; i < followRaw.length() && followUps.size() < 3; i++) {
                String text = followRaw.optString(i, "").trim();
                if (!text.isEmpty()) {
                    followUps.add(text);
                }
            }
        }
        reply.setSuggestedFollowUps(followUps);
        return reply;
    }

    private List<RagCitation> parseCitations(JSONArray citationsRaw, List<RetrievedChunk> passages) {
        Set<String> allowed = new HashSet<>();
        for (RetrievedChunk passage : passages) {
            if (passage.getChunk() != null && passage.getChunk().getNoteId() != null) {
                allowed.add(passage.getChunk().getNoteId());
            }
        }
        List<RagCitation> citations = new ArrayList<>();
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
        if (citations.isEmpty() && !passages.isEmpty() && passages.get(0).getChunk() != null) {
            citations.add(new RagCitation(
                    passages.get(0).getChunk().getNoteId(),
                    passages.get(0).getChunk().getTitle(),
                    GeminiApiClient.truncate(passages.get(0).getChunk().getText(), 180)
            ));
        }
        return citations;
    }
}
