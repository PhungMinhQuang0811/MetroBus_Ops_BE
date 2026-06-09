package com.vdt.auth_ops_service.security.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountStatusRedisService {

    static final String ACCOUNT_DISABLED_KEY_PREFIX = "auth:account:disabled:";

    RedisTemplate<String, String> redisTemplate;

    public void markDisabled(String accountId) {
        redisTemplate.opsForValue().set(ACCOUNT_DISABLED_KEY_PREFIX + accountId, "true");
    }

    public void markEnabled(String accountId) {
        redisTemplate.delete(ACCOUNT_DISABLED_KEY_PREFIX + accountId);
    }
}
