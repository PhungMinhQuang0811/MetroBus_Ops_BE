package com.vdt.authservice.security.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TokenManagementService {
    RedisTemplate<String, String> redisTemplate;

    private static final String INVALIDATED_ACCESS_TOKEN_PREFIX = "accessTk:";
    private static final String INVALIDATED_REFRESH_TOKEN_PREFIX = "refreshTk:";
    private static final String INVALIDATED_CSRF_TOKEN_PREFIX = "csrfTk:";

    public void invalidateAccessToken(String token, Instant expireAt){
        String key = INVALIDATED_ACCESS_TOKEN_PREFIX + token;
        //set key
        redisTemplate.opsForValue().set(key, "");
        //set expire at
        redisTemplate.expireAt(key, expireAt);
    }

    public boolean isAccessTokenInvalidated(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(INVALIDATED_ACCESS_TOKEN_PREFIX + token));
    }

    public void invalidateRefreshToken(String token, Instant expireAt){
        String key = INVALIDATED_REFRESH_TOKEN_PREFIX + token;
        //set key
        redisTemplate.opsForValue().set(key, "");
        //set expire at
        redisTemplate.expireAt(key, expireAt);
    }

    public boolean isRefreshTokenInvalidated(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(INVALIDATED_REFRESH_TOKEN_PREFIX + token));
    }

    public void invalidateCsrfToken(String token, Instant expireAt){
        String key = INVALIDATED_CSRF_TOKEN_PREFIX + token;
        //set key
        redisTemplate.opsForValue().set(key, "");
        //set expire at
        redisTemplate.expireAt(key, expireAt);
    }

    public boolean isCsrfTokenInvalidated(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(INVALIDATED_CSRF_TOKEN_PREFIX + token));
    }
}
