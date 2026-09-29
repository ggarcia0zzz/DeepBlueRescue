package com.deepblue.rescue.shared;

import java.util.Locale;

public class TextNormalizer {
    private TextNormalizer() {
    }

    public static String code(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public static String optionalCode(String value) {
        return (value == null || value.isBlank()) ? null : code(value);
    }

    public static String email(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public static String text(String value) {
        return value.trim();
    }

    public static String optionalText(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}