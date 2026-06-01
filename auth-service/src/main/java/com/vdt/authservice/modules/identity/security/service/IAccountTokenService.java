package com.vdt.authservice.modules.identity.security.service;

public interface IAccountTokenService {
    String generateVerificationToken(String accountId);
    String getExistingVerificationToken(String accountId);
    String getAccountIdByVerificationToken(String token);
    void deleteVerificationToken(String token);
    String generateResetPasswordToken(String accountId);
    String getAccountIdByResetPasswordToken(String token);
    void deleteResetPasswordToken(String token);
}
