package com.vdt.authservice.security.service;

import com.vdt.authservice.modules.identity.security.service.Impl.TokenManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenManagementServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenManagementService tokenManagementService;

    private final String token = "test-token";
    private final Instant expireAt = Instant.now().plusSeconds(3600);

    @Test
    void invalidateAccessToken_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        tokenManagementService.invalidateAccessToken(token, expireAt);
        verify(valueOperations, times(1)).set("accessTk:" + token, "");
        verify(redisTemplate, times(1)).expireAt("accessTk:" + token, expireAt);
    }

    @Test
    void isAccessTokenInvalidated_ReturnsTrue() {
        when(redisTemplate.hasKey("accessTk:" + token)).thenReturn(true);
        assertTrue(tokenManagementService.isAccessTokenInvalidated(token));
    }

    @Test
    void isAccessTokenInvalidated_ReturnsFalse() {
        when(redisTemplate.hasKey("accessTk:" + token)).thenReturn(false);
        assertFalse(tokenManagementService.isAccessTokenInvalidated(token));
    }

    @Test
    void invalidateRefreshToken_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        tokenManagementService.invalidateRefreshToken(token, expireAt);
        verify(valueOperations, times(1)).set("refreshTk:" + token, "");
        verify(redisTemplate, times(1)).expireAt("refreshTk:" + token, expireAt);
    }

    @Test
    void isRefreshTokenInvalidated_ReturnsTrue() {
        when(redisTemplate.hasKey("refreshTk:" + token)).thenReturn(true);
        assertTrue(tokenManagementService.isRefreshTokenInvalidated(token));
    }

    @Test
    void invalidateCsrfToken_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        tokenManagementService.invalidateCsrfToken(token, expireAt);
        verify(valueOperations, times(1)).set("csrfTk:" + token, "");
        verify(redisTemplate, times(1)).expireAt("csrfTk:" + token, expireAt);
    }

    @Test
    void isCsrfTokenInvalidated_ReturnsTrue() {
        when(redisTemplate.hasKey("csrfTk:" + token)).thenReturn(true);
        assertTrue(tokenManagementService.isCsrfTokenInvalidated(token));
    }
}
