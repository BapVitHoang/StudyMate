package com.hcmute.studymate.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Constants {
    public static final String CATEGORY_ALL = "All";
    public static final String CATEGORY_GENERAL = "General";
    public static final String SUMMARY_SOURCE_LOCAL = "local-rule-based";
    public static final String STATUS_NEW = "New";
    public static final String STATUS_LEARNING = "Learning";
    public static final String STATUS_REVIEWED = "Reviewed";
    public static final String STATUS_MASTERED = "Mastered";

    public static final List<String> DEFAULT_CATEGORY_NAMES = Collections.unmodifiableList(
            Arrays.asList(CATEGORY_GENERAL, "Math", "English", "Programming")
    );

    public static final List<String> NOTE_STATUSES = Collections.unmodifiableList(
            Arrays.asList(STATUS_NEW, STATUS_LEARNING, STATUS_REVIEWED, STATUS_MASTERED)
    );

    private Constants() {
    }
}
