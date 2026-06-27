package com.hcmute.studymate.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Constants {
    public static final String CATEGORY_ALL = "All";
    public static final String CATEGORY_GENERAL = "General";
    public static final String SUMMARY_SOURCE_LOCAL = "local-rule-based";

    public static final List<String> DEFAULT_CATEGORY_NAMES = Collections.unmodifiableList(
            Arrays.asList(CATEGORY_GENERAL, "Math", "English", "Programming")
    );

    private Constants() {
    }
}
