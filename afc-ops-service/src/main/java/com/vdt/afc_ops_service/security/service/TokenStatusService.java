package com.vdt.afc_ops_service.security.service;

import com.vdt.afc_ops_service.constant.SecurityConstants;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TokenStatusService {

    static final String INVALIDATED_ACCESS_TOKEN_PREFIX = "accessTk:";

    RedisTemplate<String, String> redisTemplate;

    public boolean isAccessTokenInvalidated(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(INVALIDATED_ACCESS_TOKEN_PREFIX + token));
    }

    public boolean isAccountDisabled(String accountId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(SecurityConstants.ACCOUNT_DISABLED_KEY_PREFIX + accountId));
    }
}
