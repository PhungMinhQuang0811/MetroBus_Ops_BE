package com.vdt.authservice.util;

import com.vdt.authservice.common.exception.AppException;
import com.vdt.authservice.common.exception.ErrorCode;
import com.vdt.authservice.common.util.PhoneNumberUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhoneNumberUtilsTest {

    @Test
    void normalize_Null_ThrowsException() {
        AppException ex = assertThrows(AppException.class, () -> PhoneNumberUtil.normalize(null));

        assertEquals(ErrorCode.INVALID_PHONE_NUMBER, ex.getErrorCode());
    }

    @Test
    void normalize_InvalidFormat_ThrowsException() {
        AppException ex = assertThrows(AppException.class, () -> PhoneNumberUtil.normalize("09000000001"));

        assertEquals(ErrorCode.INVALID_PHONE_NUMBER, ex.getErrorCode());
    }

    @Test
    void normalize_TrimsAndReturnsLocalPhone() {
        assertEquals("0900000001", PhoneNumberUtil.normalize(" 0900000001 "));
    }

    @Test
    void normalize_ConvertsInternationalPhone() {
        assertEquals("0900000001", PhoneNumberUtil.normalize("+84900000001"));
    }
}
