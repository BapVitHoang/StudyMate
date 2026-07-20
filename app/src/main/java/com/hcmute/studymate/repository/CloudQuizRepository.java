package com.hcmute.studymate.repository;

import com.hcmute.studymate.ai.GeminiApiClient;
import com.hcmute.studymate.ai.GeminiPromptBuilder;
import com.hcmute.studymate.model.QuizQuestion;
import com.hcmute.studymate.model.RetrievedChunk;
import com.hcmute.studymate.utils.DataCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CloudQuizRepository {
    private final GeminiApiClient geminiApiClient;

    public CloudQuizRepository() {
        this(GeminiApiClient.getInstance());
    }

    CloudQuizRepository(GeminiApiClient geminiApiClient) {
        this.geminiApiClient = geminiApiClient;
    }

    public void generate(List<RetrievedChunk> passages, int questionCount, List<String> types,
                         String locale, DataCallback<List<QuizQuestion>> callback) {
        if (passages == null || passages.isEmpty()) {
            callback.onError(new IllegalArgumentException("At least one passage is required"));
            return;
        }
        String typesCsv = types == null || types.isEmpty() ? "mcq, short" : String.join(", ", types);
        String safeLocale = locale == null ? "en" : locale;
        int count = questionCount <= 0 ? 5 : Math.min(questionCount, 10);
        String prompt = GeminiPromptBuilder.quiz(safeLocale, count, typesCsv, passages);
        geminiApiClient.generateJsonAsync(prompt, 0.35, new GeminiApiClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                try {
                    callback.onSuccess(parse(json, passages));
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

    private List<QuizQuestion> parse(JSONObject data, List<RetrievedChunk> passages) throws Exception {
        Set<String> allowed = new HashSet<>();
        for (RetrievedChunk passage : passages) {
            if (passage.getChunk() != null && passage.getChunk().getNoteId() != null) {
                allowed.add(passage.getChunk().getNoteId());
            }
        }

        List<QuizQuestion> questions = new ArrayList<>();
        JSONArray questionsRaw = data.optJSONArray("questions");
        if (questionsRaw != null) {
            for (int i = 0; i < questionsRaw.length() && questions.size() < 10; i++) {
                JSONObject item = questionsRaw.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String stem = item.optString("stem", "").trim();
                String answer = item.optString("answer", "").trim();
                String sourceNoteId = item.optString("sourceNoteId", "").trim();
                if (stem.isEmpty() || answer.isEmpty() || !allowed.contains(sourceNoteId)) {
                    continue;
                }
                QuizQuestion question = new QuizQuestion();
                question.setId(UUID.randomUUID().toString());
                String type = item.optString("type", "short").trim().toLowerCase();
                question.setType(QuizQuestion.TYPE_MCQ.equals(type) ? QuizQuestion.TYPE_MCQ : QuizQuestion.TYPE_SHORT);
                question.setStem(stem);
                question.setAnswer(answer);
                question.setExplanation(item.optString("explanation", ""));
                question.setSourceNoteId(sourceNoteId);
                question.setSourceChunkId(item.optString("sourceChunkId", ""));
                List<String> choices = new ArrayList<>();
                JSONArray choicesRaw = item.optJSONArray("choices");
                if (choicesRaw != null) {
                    for (int c = 0; c < choicesRaw.length() && choices.size() < 4; c++) {
                        String choice = choicesRaw.optString(c, "").trim();
                        if (!choice.isEmpty()) {
                            choices.add(choice);
                        }
                    }
                }
                question.setChoices(QuizQuestion.TYPE_MCQ.equals(question.getType()) ? choices : new ArrayList<>());
                questions.add(question);
            }
        }
        if (questions.isEmpty()) {
            throw new IllegalStateException("No valid grounded quiz questions returned");
        }
        return questions;
    }
}
