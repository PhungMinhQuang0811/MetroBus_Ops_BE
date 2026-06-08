package com.vdt.auth_ops_service.common.util;

import java.security.SecureRandom;

public final class PasswordUtil {
    private static final int TEMPORARY_PASSWORD_LENGTH = 9;
    private static final String UPPERCASE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE_CHARS = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGIT_CHARS = "23456789";
    private static final String TEMPORARY_PASSWORD_CHARS = UPPERCASE_CHARS + LOWERCASE_CHARS + DIGIT_CHARS;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(TEMPORARY_PASSWORD_LENGTH);

        password.append(randomChar(UPPERCASE_CHARS));
        password.append(randomChar(LOWERCASE_CHARS));
        password.append(randomChar(DIGIT_CHARS));

        for (int i = password.length(); i < TEMPORARY_PASSWORD_LENGTH; i++) {
            password.append(randomChar(TEMPORARY_PASSWORD_CHARS));
        }

        shuffle(password);

        return password.toString();
    }

    private static char randomChar(String chars) {
        return chars.charAt(SECURE_RANDOM.nextInt(chars.length()));
    }

    private static void shuffle(StringBuilder value) {
        for (int i = value.length() - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char current = value.charAt(i);
            value.setCharAt(i, value.charAt(j));
            value.setCharAt(j, current);
        }
    }
}
