package com.onlineexamsystem.backend.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class PayloadUtils {

    private PayloadUtils() {
    }

    public static String stringValue(Map<String, Object> payload, String key) {
        return stringValue(payload.get(key), "");
    }

    public static String stringValue(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : text;
    }

    public static String nullableString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    public static Integer integerValue(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public static Long longValue(Object value, Long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public static boolean booleanValue(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = String.valueOf(value).trim().toLowerCase();
        if (text.isEmpty()) {
            return defaultValue;
        }
        if (List.of("1", "true", "yes", "on").contains(text)) {
            return true;
        }
        if (List.of("0", "false", "no", "off").contains(text)) {
            return false;
        }
        return defaultValue;
    }

    public static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (Object item : list) {
            String text = item == null ? "" : String.valueOf(item).trim();
            if (!text.isEmpty()) {
                values.add(text);
            }
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> objectMapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                items.add((Map<String, Object>) map);
            }
        }
        return items;
    }
}
