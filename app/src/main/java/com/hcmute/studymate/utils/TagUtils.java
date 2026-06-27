package com.hcmute.studymate.utils;

import java.util.ArrayList;
import java.util.List;

public final class TagUtils {
    private TagUtils() {
    }

    public static List<String> parseCsv(String value) {
        List<String> tags = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return tags;
        }

        String[] parts = value.split(",");
        for (String part : parts) {
            String tag = part.trim();
            if (!tag.isEmpty()) {
                tags.add(tag);
            }
        }
        return tags;
    }

    public static String join(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String tag : tags) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(tag);
        }
        return builder.toString();
    }
}
