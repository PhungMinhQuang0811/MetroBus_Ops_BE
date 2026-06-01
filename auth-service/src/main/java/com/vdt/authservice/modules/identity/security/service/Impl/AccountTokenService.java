package com.vdt.authservice.modules.identity.security.service.Impl;

import com.vdt.authservice.modules.identity.security.service.IAccountTokenService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.vdt.authservice.common.util.RedisUtil;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountTokenService implements IAccountTokenService {
    RedisUtil redisUtil;
    
    static final String VERIFICATION_PREFIX = "verification:";
    static final String VERIFICATION_USER_PREFIX = "verification_user:";
    static final String RESET_PASSWORD_PREFIX = "reset-password:";
    static final long TOKEN_TTL_DAYS = 1;

    @Override
    public String generateVerificationToken(String accountId) {
        String token = UUID.randomUUID().toString();
        redisUtil.set(VERIFICATION_PREFIX + token, accountId, TOKEN_TTL_DAYS, TimeUnit.DAYS);
        redisUtil.set(VERIFICATION_USER_PREFIX + accountId, token, TOKEN_TTL_DAYS, TimeUnit.DAYS);
        return token;
    }

    @Override
    public String getExistingVerificationToken(String accountId) {
        return redisUtil.get(VERIFICATION_USER_PREFIX + accountId);
    }

    @Override
    public String getAccountIdByVerificationToken(String token) {
        return redisUtil.get(VERIFICATION_PREFIX + token);
    }

    @Override
    public void deleteVerificationToken(String token) {
        String accountId = getAccountIdByVerificationToken(token);
        if (accountId != null) {
            redisUtil.delete(VERIFICATION_USER_PREFIX + accountId);
        }
        redisUtil.delete(VERIFICATION_PREFIX + token);
    }

    @Override
    public String generateResetPasswordToken(String accountId) {
        String token = UUID.randomUUID().toString();
        redisUtil.set(RESET_PASSWORD_PREFIX + token, accountId, TOKEN_TTL_DAYS, TimeUnit.DAYS);
        return token;
    }

    @Override
    public String getAccountIdByResetPasswordToken(String token) {
        return redisUtil.get(RESET_PASSWORD_PREFIX + token);
    }

    @Override
    public void deleteResetPasswordToken(String token) {
        redisUtil.delete(RESET_PASSWORD_PREFIX + token);
    }
}
