package com.vdt.authservice.modules.identity.service.Impl;

import com.vdt.authservice.common.exception.AppException;
import com.vdt.authservice.common.exception.ErrorCode;
import com.vdt.authservice.common.notification.sms.ISmsService;
import com.vdt.authservice.common.util.RedisUtil;
import com.vdt.authservice.modules.identity.service.IOtpService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OtpService implements IOtpService {
    private static final String OTP_KEY_PREFIX = "auth:otp:phone:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0\\d{9}|\\+84\\d{9})$");

    RedisUtil redisUtil;
    ISmsService smsService;

    @NonFinal
    @Value("${app.security.otp.expiration:120000}")
    long otpExpiration;

    @Override
    public void requestOtp(String phoneNumber) {
        String normalizedPhone = normalizePhoneNumber(phoneNumber);
        String otp = generateOtp();
        redisUtil.set(buildOtpKey(normalizedPhone), otp, otpExpiration, TimeUnit.MILLISECONDS);
        smsService.sendOtp(normalizedPhone, otp, otpExpiration);
    }

    @Override
    public void verifyOtp(String phoneNumber, String otp) {
        String normalizedPhone = normalizePhoneNumber(phoneNumber);
        String cachedOtp = redisUtil.get(buildOtpKey(normalizedPhone));

        if (cachedOtp == null || !cachedOtp.equals(otp)) {
            throw new AppException(ErrorCode.OTP_INVALID_OR_EXPIRED);
        }

        redisUtil.delete(buildOtpKey(normalizedPhone));
    }

    @Override
    public String normalizePhoneNumber(String phoneNumber) {
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

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private String buildOtpKey(String phoneNumber) {
        return OTP_KEY_PREFIX + phoneNumber;
    }
}
