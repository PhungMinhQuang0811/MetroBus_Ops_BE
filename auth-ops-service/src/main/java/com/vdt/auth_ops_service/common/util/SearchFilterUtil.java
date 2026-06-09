package com.vdt.auth_ops_service.common.util;

public final class SearchFilterUtil {
    private SearchFilterUtil() {
    }

    public static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static String toKeywordPattern(String keyword) {
        return keyword == null ? "%" : "%" + keyword.toLowerCase() + "%";
    }
}
