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
        ReflectionTestUtils.setField(otpService, "resendCooldownSeconds", 60L);
        ReflectionTestUtils.setField(otpService, "maxRequestsPerPhonePerDay", 5L);
        ReflectionTestUtils.setField(otpService, "maxVerificationAttempts", 5L);
        ReflectionTestUtils.setField(otpService, "timezone", "Asia/Ho_Chi_Minh");
    }

    @Test
    void requestOtp_ValidLocalPhone_StoresOtpAndSendsSms() {
        when(redisUtil.buildOtpCooldownKey("0900000001")).thenReturn("auth:otp:cooldown:phone:0900000001");
        when(redisUtil.buildOtpPhoneRateLimitKey("0900000001")).thenReturn("auth:otp:rate:phone:day:0900000001");
        when(redisUtil.buildOtpKey("0900000001")).thenReturn("auth:otp:phone:0900000001");
        when(redisUtil.buildOtpVerifyAttemptKey("0900000001")).thenReturn("auth:otp:verify-attempt:phone:0900000001");
        when(redisUtil.hasKey("auth:otp:cooldown:phone:0900000001")).thenReturn(false);
        when(redisUtil.increment("auth:otp:rate:phone:day:0900000001")).thenReturn(1L);

        otpService.requestOtp("0900000001");

        verify(redisUtil).expire("auth:otp:rate:phone:day:0900000001", 1, TimeUnit.DAYS);
        verify(redisUtil).set(eq("auth:otp:phone:0900000001"), matches("\\d{6}"), eq(120000L), eq(TimeUnit.MILLISECONDS));
        verify(redisUtil).set("auth:otp:cooldown:phone:0900000001", "1", 60L, TimeUnit.SECONDS);
        verify(smsService).sendOtp(eq("0900000001"), matches("\\d{6}"), eq(120000L));
    }

    @Test
    void requestOtp_ValidInternationalPhone_NormalizesBeforeStoreAndSend() {
        when(redisUtil.buildOtpCooldownKey("0900000001")).thenReturn("auth:otp:cooldown:phone:0900000001");
        when(redisUtil.buildOtpPhoneRateLimitKey("0900000001")).thenReturn("auth:otp:rate:phone:day:0900000001");
        when(redisUtil.buildOtpKey("0900000001")).thenReturn("auth:otp:phone:0900000001");
        when(redisUtil.buildOtpVerifyAttemptKey("0900000001")).thenReturn("auth:otp:verify-attempt:phone:0900000001");
        when(redisUtil.hasKey("auth:otp:cooldown:phone:0900000001")).thenReturn(false);
        when(redisUtil.increment("auth:otp:rate:phone:day:0900000001")).thenReturn(2L);

        otpService.requestOtp("+84900000001");

        verify(redisUtil).set(eq("auth:otp:phone:0900000001"), matches("\\d{6}"), eq(120000L), eq(TimeUnit.MILLISECONDS));
        verify(smsService).sendOtp(eq("0900000001"), matches("\\d{6}"), eq(120000L));
    }

    @Test
    void requestOtp_RateLimited_ThrowsExceptionWithRetryTimeAndDoesNotSendOtp() {
        when(redisUtil.buildOtpCooldownKey("0900000001")).thenReturn("auth:otp:cooldown:phone:0900000001");
        when(redisUtil.buildOtpPhoneRateLimitKey("0900000001")).thenReturn("auth:otp:rate:phone:day:0900000001");
        when(redisUtil.hasKey("auth:otp:cooldown:phone:0900000001")).thenReturn(false);
        when(redisUtil.increment("auth:otp:rate:phone:day:0900000001")).thenReturn(6L);
        when(redisUtil.getExpire("auth:otp:rate:phone:day:0900000001", TimeUnit.MILLISECONDS)).thenReturn(3_600_000L);

        AppException ex = assertThrows(AppException.class, () -> otpService.requestOtp("0900000001"));

        assertEquals(ErrorCode.OTP_DAILY_LIMIT_REACHED, ex.getErrorCode());
        assertTrue(ex.getMessage().startsWith("OTP request limit reached. You can request up to 5 OTPs within 24 hours."));
        verify(redisUtil, never()).set(startsWith("auth:otp:phone:"), anyString(), anyLong(), any());
        verify(smsService, never()).sendOtp(anyString(), anyString(), anyLong());
    }

    @Test
    void requestOtp_DuringCooldown_ThrowsExceptionAndDoesNotIncrementDailyLimit() {
        when(redisUtil.buildOtpCooldownKey("0900000001")).thenReturn("auth:otp:cooldown:phone:0900000001");
        when(redisUtil.hasKey("auth:otp:cooldown:phone:0900000001")).thenReturn(true);
        when(redisUtil.getExpire("auth:otp:cooldown:phone:0900000001", TimeUnit.MILLISECONDS)).thenReturn(30_000L);

        AppException ex = assertThrows(AppException.class, () -> otpService.requestOtp("0900000001"));

        assertEquals(ErrorCode.OTP_RESEND_COOLDOWN, ex.getErrorCode());
        assertTrue(ex.getMessage().startsWith("OTP was sent recently."));
        verify(redisUtil, never()).increment(anyString());
        verify(smsService, never()).sendOtp(anyString(), anyString(), anyLong());
    }

    @Test
    void requestOtp_DailyCounterIncrementFailed_ThrowsOtpSendFailed() {
        when(redisUtil.buildOtpCooldownKey("0900000001")).thenReturn("auth:otp:cooldown:phone:0900000001");
        when(redisUtil.buildOtpPhoneRateLimitKey("0900000001")).thenReturn("auth:otp:rate:phone:day:0900000001");
        when(redisUtil.hasKey("auth:otp:cooldown:phone:0900000001")).thenReturn(false);
        when(redisUtil.increment("auth:otp:rate:phone:day:0900000001")).thenReturn(null);

        AppException ex = assertThrows(AppException.class, () -> otpService.requestOtp("0900000001"));

        assertEquals(ErrorCode.OTP_SEND_FAILED, ex.getErrorCode());
        verify(smsService, never()).sendOtp(anyString(), anyString(), anyLong());
    }

    @Test
    void requestOtp_RateLimitedWithoutTtl_FallsBackToFullWindow() {
        when(redisUtil.buildOtpCooldownKey("0900000001")).thenReturn("auth:otp:cooldown:phone:0900000001");
        when(redisUtil.buildOtpPhoneRateLimitKey("0900000001")).thenReturn("auth:otp:rate:phone:day:0900000001");
        when(redisUtil.hasKey("auth:otp:cooldown:phone:0900000001")).thenReturn(false);
        when(redisUtil.increment("auth:otp:rate:phone:day:0900000001")).thenReturn(6L);
        when(redisUtil.getExpire("auth:otp:rate:phone:day:0900000001", TimeUnit.MILLISECONDS)).thenReturn(null);

        AppException ex = assertThrows(AppException.class, () -> otpService.requestOtp("0900000001"));

        assertEquals(ErrorCode.OTP_DAILY_LIMIT_REACHED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("within 24 hours"));
    }

    @Test
    void verifyOtp_MatchingOtp_DeletesOtp() {
        when(redisUtil.buildOtpKey("0900000001")).thenReturn("auth:otp:phone:0900000001");
        when(redisUtil.buildOtpVerifyAttemptKey("0900000001")).thenReturn("auth:otp:verify-attempt:phone:0900000001");
        when(redisUtil.get("auth:otp:phone:0900000001")).thenReturn("123456");

        otpService.verifyOtp("0900000001", "123456");

        verify(redisUtil).delete("auth:otp:phone:0900000001");
    }

    @Test
    void verifyOtp_MissingOtp_ThrowsException() {
        when(redisUtil.buildOtpKey("0900000001")).thenReturn("auth:otp:phone:0900000001");
        when(redisUtil.buildOtpVerifyAttemptKey("0900000001")).thenReturn("auth:otp:verify-attempt:phone:0900000001");
        when(redisUtil.get("auth:otp:phone:0900000001")).thenReturn(null);
        when(redisUtil.increment("auth:otp:verify-attempt:phone:0900000001")).thenReturn(1L);

        AppException ex = assertThrows(AppException.class, () -> otpService.verifyOtp("0900000001", "123456"));

        assertEquals(ErrorCode.OTP_INVALID_OR_EXPIRED, ex.getErrorCode());
        verify(redisUtil).expire("auth:otp:verify-attempt:phone:0900000001", 120000L, TimeUnit.MILLISECONDS);
        verify(redisUtil, never()).delete("auth:otp:phone:0900000001");
    }

    @Test
    void verifyOtp_NotMatchingOtp_ThrowsException() {
        when(redisUtil.buildOtpKey("0900000001")).thenReturn("auth:otp:phone:0900000001");
        when(redisUtil.buildOtpVerifyAttemptKey("0900000001")).thenReturn("auth:otp:verify-attempt:phone:0900000001");
        when(redisUtil.get("auth:otp:phone:0900000001")).thenReturn("654321");
        when(redisUtil.increment("auth:otp:verify-attempt:phone:0900000001")).thenReturn(1L);

        AppException ex = assertThrows(AppException.class, () -> otpService.verifyOtp("0900000001", "123456"));

        assertEquals(ErrorCode.OTP_INVALID_OR_EXPIRED, ex.getErrorCode());
        verify(redisUtil).expire("auth:otp:verify-attempt:phone:0900000001", 120000L, TimeUnit.MILLISECONDS);
        verify(redisUtil, never()).delete("auth:otp:phone:0900000001");
    }

    @Test
    void verifyOtp_InvalidOtpAtMaxAttempts_DeletesOtpAndAttemptCounter() {
        when(redisUtil.buildOtpKey("0900000001")).thenReturn("auth:otp:phone:0900000001");
        when(redisUtil.buildOtpVerifyAttemptKey("0900000001")).thenReturn("auth:otp:verify-attempt:phone:0900000001");
        when(redisUtil.get("auth:otp:phone:0900000001")).thenReturn("654321");
        when(redisUtil.increment("auth:otp:verify-attempt:phone:0900000001")).thenReturn(5L);

        AppException ex = assertThrows(AppException.class, () -> otpService.verifyOtp("0900000001", "123456"));

        assertEquals(ErrorCode.OTP_INVALID_OR_EXPIRED, ex.getErrorCode());
        verify(redisUtil).delete("auth:otp:phone:0900000001");
        verify(redisUtil).delete("auth:otp:verify-attempt:phone:0900000001");
    }

    @Test
    void verifyOtp_AttemptIncrementFailed_ThrowsInvalidOtp() {
        when(redisUtil.buildOtpKey("0900000001")).thenReturn("auth:otp:phone:0900000001");
        when(redisUtil.buildOtpVerifyAttemptKey("0900000001")).thenReturn("auth:otp:verify-attempt:phone:0900000001");
        when(redisUtil.get("auth:otp:phone:0900000001")).thenReturn("654321");
        when(redisUtil.increment("auth:otp:verify-attempt:phone:0900000001")).thenReturn(null);

        AppException ex = assertThrows(AppException.class, () -> otpService.verifyOtp("0900000001", "123456"));

        assertEquals(ErrorCode.OTP_INVALID_OR_EXPIRED, ex.getErrorCode());
        verify(redisUtil, never()).expire(anyString(), anyLong(), any());
        verify(redisUtil, never()).delete(anyString());
    }

}
