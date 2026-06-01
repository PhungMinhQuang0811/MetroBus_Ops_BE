package com.vdt.authservice.common.notification.sms;

public interface ISmsService {
    void sendSms(String phoneNumber, String message);

    void sendOtp(String phoneNumber, String otp, long expirationMillis);
}
