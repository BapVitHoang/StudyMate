package com.hcmute.studymate.repository;

import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.hcmute.studymate.model.QuizQuestion;
import com.hcmute.studymate.model.RetrievedChunk;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.PassagePayloadMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CloudQuizRepository {
    private static final String FUNCTIONS_REGION = "asia-southeast1";
    private final FirebaseFunctions functions;

    public CloudQuizRepository() {
        this(FirebaseFunctions.getInstance(FUNCTIONS_REGION));
    }

    CloudQuizRepository(FirebaseFunctions functions) {
        this.functions = functions;
    }

    public void generate(List<RetrievedChunk> passages, int questionCount, List<String> types,
                         String locale, DataCallback<List<QuizQuestion>> callback) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("passages", PassagePayloadMapper.toPayload(passages));
        payload.put("questionCount", questionCount);
        payload.put("types", types);
        payload.put("locale", locale == null ? "en" : locale);

        functions.getHttpsCallable(Constants.CALLABLE_GENERATE_QUIZ)
                .call(payload)
                .addOnSuccessListener(result -> {
                    try {
                        callback.onSuccess(parse(result));
                    } catch (Exception exception) {
                        callback.onError(exception);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    @SuppressWarnings("unchecked")
    private List<QuizQuestion> parse(HttpsCallableResult callableResult) {
        Object raw = callableResult.getData();
        if (!(raw instanceof Map)) {
            throw new IllegalStateException("Unexpected quiz response");
        }
        Map<String, Object> data = (Map<String, Object>) raw;
        List<QuizQuestion> questions = new ArrayList<>();
        Object questionsRaw = data.get("questions");
        if (questionsRaw instanceof List) {
            for (Object item : (List<?>) questionsRaw) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<String, Object> map = (Map<String, Object>) item;
                QuizQuestion question = new QuizQuestion();
                question.setId(UUID.randomUUID().toString());
                question.setType(asString(map.get("type")));
                question.setStem(asString(map.get("stem")));
                question.setAnswer(asString(map.get("answer")));
                question.setExplanation(asString(map.get("explanation")));
                question.setSourceNoteId(asString(map.get("sourceNoteId")));
                question.setSourceChunkId(asString(map.get("sourceChunkId")));
                List<String> choices = new ArrayList<>();
                Object choicesRaw = map.get("choices");
                if (choicesRaw instanceof List) {
                    for (Object choice : (List<?>) choicesRaw) {
                        if (choice != null) {
                            choices.add(String.valueOf(choice));
                        }
                    }
                }
                question.setChoices(choices);
                questions.add(question);
            }
        }
        if (questions.isEmpty()) {
            throw new IllegalStateException("No quiz questions returned");
        }
        return questions;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }
}
