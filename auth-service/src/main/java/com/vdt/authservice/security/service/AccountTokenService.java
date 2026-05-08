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
    
    static final String VERIFICATION_PREFIX = "verification:";
    static final String VERIFICATION_USER_PREFIX = "verification_user:";
    static final String RESET_PASSWORD_PREFIX = "reset-password:";
    static final long TOKEN_TTL_DAYS = 1;

    public String generateVerificationToken(String accountId) {
        String token = UUID.randomUUID().toString();
        redisUtil.set(VERIFICATION_PREFIX + token, accountId, TOKEN_TTL_DAYS, TimeUnit.DAYS);
        redisUtil.set(VERIFICATION_USER_PREFIX + accountId, token, TOKEN_TTL_DAYS, TimeUnit.DAYS);
        return token;
    }

    public String getExistingVerificationToken(String accountId) {
        return redisUtil.get(VERIFICATION_USER_PREFIX + accountId);
    }

    public String getAccountIdByVerificationToken(String token) {
        return redisUtil.get(VERIFICATION_PREFIX + token);
    }

    public void deleteVerificationToken(String token) {
        String accountId = getAccountIdByVerificationToken(token);
        if (accountId != null) {
            redisUtil.delete(VERIFICATION_USER_PREFIX + accountId);
        }
        redisUtil.delete(VERIFICATION_PREFIX + token);
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
