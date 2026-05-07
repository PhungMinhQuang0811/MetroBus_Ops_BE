package com.vdt.authservice.security.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.vdt.authservice.util.RedisUtil;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountTokenService {
    RedisUtil redisUtil;
    
    static final String ACTIVATION_PREFIX = "activation:";
    static final String RESET_PASSWORD_PREFIX = "reset-password:";
    static final long TOKEN_TTL_DAYS = 1;

    public String generateActivationToken(String accountId) {
        String token = UUID.randomUUID().toString();
        redisUtil.set(ACTIVATION_PREFIX + token, accountId, TOKEN_TTL_DAYS, TimeUnit.DAYS);
        return token;
    }

    public String getAccountIdByActivationToken(String token) {
        return redisUtil.get(ACTIVATION_PREFIX + token);
    }

    public void deleteActivationToken(String token) {
        redisUtil.delete(ACTIVATION_PREFIX + token);
    }

    public String generateResetPasswordToken(String accountId) {
        String token = UUID.randomUUID().toString();
        redisUtil.set(RESET_PASSWORD_PREFIX + token, accountId, TOKEN_TTL_DAYS, TimeUnit.DAYS);
        return token;
    }

    public String getAccountIdByResetPasswordToken(String token) {
        return redisUtil.get(RESET_PASSWORD_PREFIX + token);
    }

    public void deleteResetPasswordToken(String token) {
        redisUtil.delete(RESET_PASSWORD_PREFIX + token);
    }
}
