package com.vdt.authservice.modules.identity.service;

public interface IOtpService {
    void requestOtp(String phoneNumber);
    void verifyOtp(String phoneNumber, String otp);
    String normalizePhoneNumber(String phoneNumber);
}
