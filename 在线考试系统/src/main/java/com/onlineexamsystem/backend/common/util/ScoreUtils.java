package com.onlineexamsystem.backend.common.util;

public final class ScoreUtils {

    private ScoreUtils() {
    }

    public static String scoreSegment(double score) {
        if (score < 60) {
            return "0-59";
        }
        if (score < 70) {
            return "60-69";
        }
        if (score < 80) {
            return "70-79";
        }
        if (score < 90) {
            return "80-89";
        }
        return "90-100";
    }
}
