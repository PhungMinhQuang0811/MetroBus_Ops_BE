package com.vdt.authservice.service;

import com.vdt.authservice.common.exception.AppException;
import com.vdt.authservice.common.exception.ErrorCode;
import com.vdt.authservice.common.notification.sms.ISmsService;
import com.vdt.authservice.common.util.RedisUtil;
import com.vdt.authservice.modules.identity.service.Impl.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock private RedisUtil redisUtil;
    @Mock private ISmsService smsService;

    @InjectMocks
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "otpExpiration", 120000L);
    }

    @Test
    void requestOtp_ValidLocalPhone_StoresOtpAndSendsSms() {
        otpService.requestOtp("0900000001");

        verify(redisUtil).set(eq("auth:otp:phone:0900000001"), matches("\\d{6}"), eq(120000L), eq(TimeUnit.MILLISECONDS));
        verify(smsService).sendOtp(eq("0900000001"), matches("\\d{6}"), eq(120000L));
    }

    @Test
    void requestOtp_ValidInternationalPhone_NormalizesBeforeStoreAndSend() {
        otpService.requestOtp("+84900000001");

        verify(redisUtil).set(eq("auth:otp:phone:0900000001"), matches("\\d{6}"), eq(120000L), eq(TimeUnit.MILLISECONDS));
        verify(smsService).sendOtp(eq("0900000001"), matches("\\d{6}"), eq(120000L));
    }

    @Test
    void verifyOtp_MatchingOtp_DeletesOtp() {
        when(redisUtil.get("auth:otp:phone:0900000001")).thenReturn("123456");

        otpService.verifyOtp("0900000001", "123456");

        verify(redisUtil).delete("auth:otp:phone:0900000001");
    }

    @Test
    void verifyOtp_MissingOtp_ThrowsException() {
        when(redisUtil.get("auth:otp:phone:0900000001")).thenReturn(null);

        AppException ex = assertThrows(AppException.class, () -> otpService.verifyOtp("0900000001", "123456"));

        assertEquals(ErrorCode.OTP_INVALID_OR_EXPIRED, ex.getErrorCode());
        verify(redisUtil, never()).delete(anyString());
    }

    @Test
    void verifyOtp_NotMatchingOtp_ThrowsException() {
        when(redisUtil.get("auth:otp:phone:0900000001")).thenReturn("654321");

        AppException ex = assertThrows(AppException.class, () -> otpService.verifyOtp("0900000001", "123456"));

        assertEquals(ErrorCode.OTP_INVALID_OR_EXPIRED, ex.getErrorCode());
        verify(redisUtil, never()).delete(anyString());
    }

    @Test
    void normalizePhoneNumber_Null_ThrowsException() {
        AppException ex = assertThrows(AppException.class, () -> otpService.normalizePhoneNumber(null));

        assertEquals(ErrorCode.INVALID_PHONE_NUMBER, ex.getErrorCode());
    }

    @Test
    void normalizePhoneNumber_InvalidFormat_ThrowsException() {
        AppException ex = assertThrows(AppException.class, () -> otpService.normalizePhoneNumber("09000000001"));

        assertEquals(ErrorCode.INVALID_PHONE_NUMBER, ex.getErrorCode());
    }

    @Test
    void normalizePhoneNumber_TrimsAndReturnsLocalPhone() {
        assertEquals("0900000001", otpService.normalizePhoneNumber(" 0900000001 "));
    }

    @Test
    void normalizePhoneNumber_ConvertsInternationalPhone() {
        assertEquals("0900000001", otpService.normalizePhoneNumber("+84900000001"));
    }
}
