package com.vdt.authservice.modules.identity.service.Impl;

import com.vdt.authservice.common.exception.AppException;
import com.vdt.authservice.common.exception.ErrorCode;
import com.vdt.authservice.common.notification.sms.ISmsService;
import com.vdt.authservice.common.util.PhoneNumberUtil;
import com.vdt.authservice.common.util.RedisUtil;
import com.vdt.authservice.modules.identity.service.IOtpService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OtpService implements IOtpService {
    private static final DateTimeFormatter RETRY_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    RedisUtil redisUtil;
    ISmsService smsService;

    @NonFinal
    @Value("${app.otp.expiration-ms}")
    long otpExpiration;

    @NonFinal
    @Value("${app.otp.resend-cooldown-seconds}")
    long resendCooldownSeconds;

    @NonFinal
    @Value("${app.otp.max-requests-per-phone-per-day}")
    long maxRequestsPerPhonePerDay;

    @NonFinal
    @Value("${app.otp.max-verification-attempts}")
    long maxVerificationAttempts;

    @NonFinal
    @Value("${app.timezone:Asia/Ho_Chi_Minh}")
    String timezone;

    @Override
    public void requestOtp(String phoneNumber) {
        String normalizedPhone = PhoneNumberUtil.normalize(phoneNumber);

        validateOtpCooldown(normalizedPhone);
        validatePhoneDailyRateLimit(normalizedPhone);

        String otp = generateOtp();
        redisUtil.set(redisUtil.buildOtpKey(normalizedPhone), otp, otpExpiration, TimeUnit.MILLISECONDS);
        redisUtil.set(redisUtil.buildOtpCooldownKey(normalizedPhone), "1", resendCooldownSeconds, TimeUnit.SECONDS);
        redisUtil.delete(redisUtil.buildOtpVerifyAttemptKey(normalizedPhone));
        smsService.sendOtp(normalizedPhone, otp, otpExpiration);
    }

    @Override
    public void verifyOtp(String phoneNumber, String otp) {
        String normalizedPhone = PhoneNumberUtil.normalize(phoneNumber);
        String cachedOtp = redisUtil.get(redisUtil.buildOtpKey(normalizedPhone));

        if (cachedOtp == null || !cachedOtp.equals(otp)) {
            increaseFailedVerifyAttempt(normalizedPhone);
            throw new AppException(ErrorCode.OTP_INVALID_OR_EXPIRED);
        }

        redisUtil.delete(redisUtil.buildOtpKey(normalizedPhone));
        redisUtil.delete(redisUtil.buildOtpVerifyAttemptKey(normalizedPhone));
    }

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private void validateOtpCooldown(String phoneNumber) {
        String cooldownKey = redisUtil.buildOtpCooldownKey(phoneNumber);
        if (!redisUtil.hasKey(cooldownKey)) {
            return;
        }

        // A valid cooldown key means an OTP was just sent and must not be replaced yet.
        Duration retryAfter = getRetryAfter(cooldownKey);
        throw new AppException(
                ErrorCode.OTP_RATE_LIMITED,
                "OTP was sent recently. Please try again after " + formatRetryTime(retryAfter) + "."
        );
    }

    private void validatePhoneDailyRateLimit(String phoneNumber) {
        String rateLimitKey = redisUtil.buildOtpPhoneRateLimitKey(phoneNumber);
        Long currentRequests = redisUtil.increment(rateLimitKey);

        if (currentRequests == null) {
            throw new AppException(ErrorCode.OTP_SEND_FAILED);
        }

        // The first request opens a 24-hour window; later requests reuse that same TTL.
        if (currentRequests == 1) {
            redisUtil.expire(rateLimitKey, 1, TimeUnit.DAYS);
            return;
        }

        // When the daily quota is exhausted, return the exact reset time instead of a vague retry message.
        if (currentRequests > maxRequestsPerPhonePerDay) {
            Duration retryAfter = getRetryAfter(rateLimitKey);
            throw new AppException(
                    ErrorCode.OTP_RATE_LIMITED,
                    "OTP request limit reached. You can request up to "
                            + maxRequestsPerPhonePerDay
                            + " OTPs within 24 hours. Please try again after "
                            + formatRetryTime(retryAfter)
                            + "."
            );
        }
    }

    private void increaseFailedVerifyAttempt(String phoneNumber) {
        String attemptKey = redisUtil.buildOtpVerifyAttemptKey(phoneNumber);
        Long failedAttempts = redisUtil.increment(attemptKey);

        if (failedAttempts == null) {
            throw new AppException(ErrorCode.OTP_INVALID_OR_EXPIRED);
        }

        // Failed attempts only matter while the current OTP is still alive.
        if (failedAttempts == 1) {
            redisUtil.expire(attemptKey, otpExpiration, TimeUnit.MILLISECONDS);
        }

        // After too many wrong attempts, invalidate the OTP so brute-force cannot continue.
        if (failedAttempts >= maxVerificationAttempts) {
            redisUtil.delete(redisUtil.buildOtpKey(phoneNumber));
            redisUtil.delete(attemptKey);
        }
    }

    private Duration getRetryAfter(String rateLimitKey) {
        Long ttlMillis = redisUtil.getExpire(rateLimitKey, TimeUnit.MILLISECONDS);
        if (ttlMillis == null || ttlMillis <= 0) {
            // Redis may return no TTL in edge cases; falling back to the full window is safer for users and SMS cost.
            return Duration.ofDays(1);
        }
        return Duration.ofMillis(ttlMillis);
    }

    private String formatRetryTime(Duration retryAfter) {
        ZoneId zoneId = ZoneId.of(timezone);
        return ZonedDateTime.now(zoneId)
                .plus(retryAfter)
                .format(RETRY_TIME_FORMATTER);
    }

}
