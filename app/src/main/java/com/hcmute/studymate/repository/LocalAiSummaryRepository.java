package com.hcmute.studymate.repository;

import com.hcmute.studymate.model.SummaryResult;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.SummaryCallback;

import java.util.ArrayList;
import java.util.List;

public class LocalAiSummaryRepository implements AiSummaryRepository {
    private static final int MAX_SENTENCES = 3;

    @Override
    public void summarize(String content, SummaryCallback callback) {
        if (content == null || content.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Content is empty"));
            return;
        }

        String summary = buildSummary(content.trim());
        callback.onSuccess(new SummaryResult(content, summary, Constants.SUMMARY_SOURCE_LOCAL,
                System.currentTimeMillis()));
    }

    private String buildSummary(String content) {
        String[] rawSentences = content.split("(?<=[.!?])\\s+|\\n+");
        List<String> sentences = new ArrayList<>();
        for (String sentence : rawSentences) {
            String cleaned = sentence.trim();
            if (!cleaned.isEmpty()) {
                sentences.add(cleaned);
            }
        }

        if (sentences.isEmpty()) {
            return content.length() <= 240 ? content : content.substring(0, 240) + "...";
        }

        StringBuilder builder = new StringBuilder();
        int count = Math.min(MAX_SENTENCES, sentences.size());
        for (int index = 0; index < count; index++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(sentences.get(index));
        }
        return builder.toString();
    }
}
