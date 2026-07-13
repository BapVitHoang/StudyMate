package com.hcmute.studymate.utils;

import com.hcmute.studymate.model.ChecklistItem;

import java.util.ArrayList;
import java.util.List;

public final class ChecklistUtils {
    private ChecklistUtils() {
    }

    public static List<ChecklistItem> parseLines(String value) {
        List<ChecklistItem> items = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return items;
        }

        String[] lines = value.split("\\r?\\n");
        for (String line : lines) {
            String cleaned = line.trim();
            if (cleaned.isEmpty()) {
                continue;
            }
            boolean checked = cleaned.startsWith("[x]") || cleaned.startsWith("[X]")
                    || cleaned.startsWith("- [x]") || cleaned.startsWith("- [X]");
            cleaned = cleaned.replaceFirst("^- \\[[ xX]]\\s*", "")
                    .replaceFirst("^\\[[ xX]]\\s*", "")
                    .replaceFirst("^-\\s*", "");
            if (!cleaned.isEmpty()) {
                items.add(new ChecklistItem(cleaned, checked));
            }
        }
        return items;
    }

    public static String joinLines(List<ChecklistItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (ChecklistItem item : items) {
            if (item == null || item.getText() == null || item.getText().trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(item.isChecked() ? "[x] " : "[ ] ");
            builder.append(item.getText().trim());
        }
        return builder.toString();
    }
}
