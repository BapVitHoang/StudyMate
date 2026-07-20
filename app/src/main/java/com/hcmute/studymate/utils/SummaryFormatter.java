package com.hcmute.studymate.utils;

import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.model.SummaryResult;

import java.util.ArrayList;
import java.util.List;

public final class SummaryFormatter {
    private SummaryFormatter() {
    }

    public static void applyToNote(Note note, SummaryResult result) {
        if (note == null || result == null) {
            return;
        }
        note.setSummary(result.getSummaryText());
        note.setSummaryBullets(copyList(result.getBullets()));
        note.setSummaryKeyTerms(copyList(result.getKeyTerms()));
        note.setSummaryConfidence(result.getConfidence());
        note.setSummarySource(result.getSource());
        note.setSummaryGeneratedAt(result.getGeneratedAt());
        note.setUpdatedAt(System.currentTimeMillis());
    }

    public static String formatDisplayBody(Note note) {
        if (note == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        if (note.getSummary() != null && !note.getSummary().trim().isEmpty()) {
            builder.append(note.getSummary().trim());
        }

        List<String> bullets = note.getSummaryBullets();
        if (bullets != null && !bullets.isEmpty()) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            for (String bullet : bullets) {
                if (bullet == null || bullet.trim().isEmpty()) {
                    continue;
                }
                builder.append("• ").append(bullet.trim()).append('\n');
            }
            if (builder.length() > 0 && builder.charAt(builder.length() - 1) == '\n') {
                builder.setLength(builder.length() - 1);
            }
        }

        return builder.toString();
    }

    public static String formatMetaLine(Note note) {
        if (note == null) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        if (note.getSummarySource() != null && !note.getSummarySource().trim().isEmpty()) {
            parts.add(humanizeSource(note.getSummarySource()));
        }
        if (note.getSummaryConfidence() != null) {
            int percent = (int) Math.round(clamp(note.getSummaryConfidence()) * 100);
            parts.add(percent + "% confidence");
        }
        return String.join(" · ", parts);
    }

    public static String formatKeyTerms(Note note) {
        if (note == null || note.getSummaryKeyTerms() == null || note.getSummaryKeyTerms().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String term : note.getSummaryKeyTerms()) {
            if (term == null || term.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("  ");
            }
            builder.append(term.trim());
        }
        return builder.toString();
    }

    public static boolean hasStructuredSummary(Note note) {
        if (note == null) {
            return false;
        }
        boolean hasText = note.getSummary() != null && !note.getSummary().trim().isEmpty();
        boolean hasBullets = note.getSummaryBullets() != null && !note.getSummaryBullets().isEmpty();
        return hasText || hasBullets;
    }

    private static String humanizeSource(String source) {
        if (Constants.SUMMARY_SOURCE_CLOUD.equals(source)) {
            return "Cloud AI";
        }
        if (Constants.SUMMARY_SOURCE_LOCAL.equals(source)) {
            return "Offline fallback";
        }
        return source;
    }

    private static double clamp(double value) {
        if (value < 0) {
            return 0;
        }
        if (value > 1) {
            return 1;
        }
        return value;
    }

    private static List<String> copyList(List<String> values) {
        List<String> copy = new ArrayList<>();
        if (values == null) {
            return copy;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                copy.add(value.trim());
            }
        }
        return copy;
    }
}
