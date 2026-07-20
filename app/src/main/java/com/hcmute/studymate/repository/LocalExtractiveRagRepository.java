package com.hcmute.studymate.repository;

import com.hcmute.studymate.model.RagAnswer;
import com.hcmute.studymate.model.RagCitation;
import com.hcmute.studymate.model.RetrievedChunk;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.DataCallback;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Offline extractive QA: pick sentences overlapping the question tokens from top passages.
 */
public class LocalExtractiveRagRepository {

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

        try {
            callback.onSuccess(buildAnswer(question.trim(), passages));
        } catch (Exception exception) {
            callback.onError(exception);
        }
    }

    RagAnswer buildAnswer(String question, List<RetrievedChunk> passages) {
        Set<String> tokens = tokenize(question);
        List<ScoredSentence> scored = new ArrayList<>();
        List<RagCitation> citations = new ArrayList<>();

        for (RetrievedChunk retrieved : passages) {
            if (retrieved == null || retrieved.getChunk() == null) {
                continue;
            }
            String text = safe(retrieved.getChunk().getText());
            if (text.isEmpty()) {
                continue;
            }
            String title = safe(retrieved.getChunk().getTitle());
            String noteId = retrieved.getChunk().getNoteId();
            citations.add(new RagCitation(noteId, title, truncate(text, 160)));

            for (String sentence : splitSentences(text)) {
                double overlap = overlapScore(tokens, tokenize(sentence));
                if (overlap <= 0) {
                    continue;
                }
                scored.add(new ScoredSentence(sentence, overlap + retrieved.getScore() * 0.01));
            }
        }

        scored.sort((left, right) -> Double.compare(right.score, left.score));
        StringBuilder answer = new StringBuilder();
        Set<String> used = new LinkedHashSet<>();
        for (ScoredSentence item : scored) {
            String normalized = item.sentence.toLowerCase(Locale.US);
            if (!used.add(normalized)) {
                continue;
            }
            if (answer.length() > 0) {
                answer.append(' ');
            }
            answer.append(item.sentence.trim());
            if (answer.length() >= 600 || used.size() >= 4) {
                break;
            }
        }

        if (answer.length() == 0) {
            String fallback = safe(passages.get(0).getChunk().getText());
            answer.append(truncate(fallback, 400));
        }

        String prefix = "Based on your notes (offline): ";
        RagAnswer result = new RagAnswer(
                prefix + answer,
                citations.size() > 5 ? citations.subList(0, 5) : citations,
                Constants.RAG_SOURCE_LOCAL,
                System.currentTimeMillis());
        result.setUsedFallback(true);
        return result;
    }

    private static double overlapScore(Set<String> queryTokens, Set<String> sentenceTokens) {
        if (queryTokens.isEmpty() || sentenceTokens.isEmpty()) {
            return 0;
        }
        int hits = 0;
        for (String token : queryTokens) {
            if (sentenceTokens.contains(token)) {
                hits++;
            }
        }
        return hits / (double) queryTokens.size();
    }

    private static List<String> splitSentences(String text) {
        String[] parts = text.split("(?<=[.!?。！？])\\s+|\\n+");
        List<String> sentences = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part == null ? "" : part.trim();
            if (trimmed.length() >= 20) {
                sentences.add(trimmed);
            }
        }
        if (sentences.isEmpty() && text.trim().length() > 0) {
            sentences.add(text.trim());
        }
        return sentences;
    }

    private static Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        if (text == null) {
            return tokens;
        }
        String[] parts = text.toLowerCase(Locale.US).split("[^\\p{L}\\p{N}]+");
        for (String part : parts) {
            if (part.length() < 3) {
                continue;
            }
            if (isStopWord(part)) {
                continue;
            }
            tokens.add(part);
        }
        return tokens;
    }

    private static boolean isStopWord(String token) {
        switch (token) {
            case "the":
            case "and":
            case "for":
            case "that":
            case "with":
            case "this":
            case "from":
            case "what":
            case "when":
            case "where":
            case "which":
            case "how":
            case "are":
            case "was":
            case "were":
            case "have":
            case "has":
            case "does":
            case "did":
            case "can":
            case "could":
            case "about":
            case "your":
            case "notes":
                return true;
            default:
                return false;
        }
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max).trim() + "…";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class ScoredSentence {
        private final String sentence;
        private final double score;

        private ScoredSentence(String sentence, double score) {
            this.sentence = sentence;
            this.score = score;
        }
    }
}
