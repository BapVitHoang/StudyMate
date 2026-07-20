package com.hcmute.studymate.repository;

import com.hcmute.studymate.model.SummarizeRequest;
import com.hcmute.studymate.model.SummaryResult;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.SummaryCallback;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalAiSummaryRepository implements AiSummaryRepository {
    private static final int MAX_SENTENCES = 3;
    private static final int MAX_BULLETS = 5;
    private static final int MAX_KEY_TERMS = 8;
    private static final Pattern WORD_PATTERN = Pattern.compile("[A-Za-zÀ-ỹ][A-Za-zÀ-ỹ\\-]{3,}");

    @Override
    public void summarize(SummarizeRequest request, SummaryCallback callback) {
        if (request == null || request.getContent() == null || request.getContent().trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Content is empty"));
            return;
        }

        String content = request.getContent().trim();
        List<String> sentences = extractSentences(content);
        String summaryText = buildSummary(content, sentences);
        List<String> bullets = buildBullets(sentences);
        List<String> keyTerms = extractKeyTerms(content, request.getTitle());

        SummaryResult result = new SummaryResult(content, summaryText, Constants.SUMMARY_SOURCE_LOCAL,
                System.currentTimeMillis());
        result.setBullets(bullets);
        result.setKeyTerms(keyTerms);
        result.setConfidence(estimateConfidence(content, sentences, bullets));
        callback.onSuccess(result);
    }

    private String buildSummary(String content, List<String> sentences) {
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

    private List<String> buildBullets(List<String> sentences) {
        List<String> bullets = new ArrayList<>();
        int count = Math.min(MAX_BULLETS, sentences.size());
        for (int index = 0; index < count; index++) {
            String sentence = sentences.get(index);
            if (sentence.length() > 160) {
                sentence = sentence.substring(0, 157) + "...";
            }
            bullets.add(sentence);
        }
        return bullets;
    }

    private List<String> extractKeyTerms(String content, String title) {
        Set<String> terms = new LinkedHashSet<>();
        if (title != null && !title.trim().isEmpty()) {
            Matcher titleMatcher = WORD_PATTERN.matcher(title);
            while (titleMatcher.find() && terms.size() < MAX_KEY_TERMS) {
                String word = normalizeTerm(titleMatcher.group());
                if (isUsefulTerm(word)) {
                    terms.add(word);
                }
            }
        }

        Matcher matcher = WORD_PATTERN.matcher(content);
        while (matcher.find() && terms.size() < MAX_KEY_TERMS) {
            String word = normalizeTerm(matcher.group());
            if (isUsefulTerm(word)) {
                terms.add(word);
            }
        }
        return new ArrayList<>(terms);
    }

    private List<String> extractSentences(String content) {
        String[] rawSentences = content.split("(?<=[.!?])\\s+|\\n+");
        List<String> sentences = new ArrayList<>();
        for (String sentence : rawSentences) {
            String cleaned = sentence.trim();
            if (!cleaned.isEmpty()) {
                sentences.add(cleaned);
            }
        }
        return sentences;
    }

    private double estimateConfidence(String content, List<String> sentences, List<String> bullets) {
        if (content.length() < 40) {
            return 0.35;
        }
        if (sentences.size() >= 3 && bullets.size() >= 3) {
            return 0.55;
        }
        return 0.45;
    }

    private String normalizeTerm(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isUsefulTerm(String word) {
        if (word == null || word.length() < 4) {
            return false;
        }
        switch (word) {
            case "this":
            case "that":
            case "with":
            case "from":
            case "have":
            case "will":
            case "they":
            case "them":
            case "your":
            case "about":
            case "there":
            case "their":
            case "which":
            case "would":
            case "could":
            case "should":
            case "note":
            case "notes":
            case "study":
                return false;
            default:
                return true;
        }
    }
}
