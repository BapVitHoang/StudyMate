package com.hcmute.studymate.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DateTimeUtils {
    private static final String DISPLAY_PATTERN = "dd/MM/yyyy HH:mm";
    private static final long ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private DateTimeUtils() {
    }

    public static String formatDateTime(long millis) {
        return new SimpleDateFormat(DISPLAY_PATTERN, Locale.getDefault()).format(new Date(millis));
    }

    public static long oneDayFromNow() {
        return System.currentTimeMillis() + ONE_DAY_MILLIS;
    }
}
