package com.vdt.authservice.common.notification.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LogSmsService implements ISmsService {
    @Override
    public void sendSms(String phoneNumber, String message) {
        log.info("SMS to {}: {}", phoneNumber, message);
    }

    @Override
    public void sendOtp(String phoneNumber, String otp, long expirationMillis) {
        long expirationSeconds = expirationMillis / 1000;
        sendSms(phoneNumber, "MetroBus OTP: " + otp + ". Ma co hieu luc trong " + expirationSeconds + " giay.");
    }
}
