package com.vdt.afc_ops_service.common.util;

import java.util.Locale;

public final class SearchFilterUtil {
    private SearchFilterUtil() {
    }

    public static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static String normalizeUppercase(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    public static String toKeywordPattern(String keyword) {
        String normalized = normalize(keyword);
        return normalized == null ? null : "%" + normalized.toLowerCase(Locale.ROOT) + "%";
    }
}
