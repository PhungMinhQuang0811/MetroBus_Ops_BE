package com.vdt.authservice.common.util;

import com.vdt.authservice.common.exception.AppException;
import com.vdt.authservice.common.exception.ErrorCode;

import java.util.regex.Pattern;

public class PhoneNumberUtil {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0\\d{9}|\\+84\\d{9})$");

    private PhoneNumberUtil() {
    }

    public static String normalize(String phoneNumber) {
        if (phoneNumber == null) {
            throw new AppException(ErrorCode.INVALID_PHONE_NUMBER);
        }

        String trimmed = phoneNumber.trim();
        if (!PHONE_PATTERN.matcher(trimmed).matches()) {
            throw new AppException(ErrorCode.INVALID_PHONE_NUMBER);
        }

        if (trimmed.startsWith("+84")) {
            return "0" + trimmed.substring(3);
        }

        return trimmed;
    }
}
