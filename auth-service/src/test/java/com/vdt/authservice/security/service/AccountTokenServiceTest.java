package com.vdt.authservice.security.service;

import com.vdt.authservice.util.RedisUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountTokenServiceTest {

    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private AccountTokenService accountTokenService;

    private final String accountId = "user-123";
    private final String token = "uuid-token";

    @Test
    void generateActivationToken_Success() {
        String result = accountTokenService.generateActivationToken(accountId);
        assertNotNull(result);
        verify(redisUtil, times(2)).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void getExistingActivationToken_Success() {
        when(redisUtil.get("activation_user:" + accountId)).thenReturn(token);
        assertEquals(token, accountTokenService.getExistingActivationToken(accountId));
    }

    @Test
    void getAccountIdByActivationToken_Success() {
        when(redisUtil.get("activation:" + token)).thenReturn(accountId);
        assertEquals(accountId, accountTokenService.getAccountIdByActivationToken(token));
    }

    @Test
    void deleteActivationToken_Success() {
        when(redisUtil.get("activation:" + token)).thenReturn(accountId);
        accountTokenService.deleteActivationToken(token);
        verify(redisUtil).delete("activation_user:" + accountId);
        verify(redisUtil).delete("activation:" + token);
    }

    @Test
    void deleteActivationToken_NotFound_StillDeletesToken() {
        when(redisUtil.get("activation:" + token)).thenReturn(null);
        accountTokenService.deleteActivationToken(token);
        verify(redisUtil, never()).delete(startsWith("activation_user:"));
        verify(redisUtil).delete("activation:" + token);
    }

    @Test
    void generateResetPasswordToken_Success() {
        String result = accountTokenService.generateResetPasswordToken(accountId);
        assertNotNull(result);
        verify(redisUtil).set(contains("reset-password:"), eq(accountId), anyLong(), any());
    }

    @Test
    void getAccountIdByResetPasswordToken_Success() {
        when(redisUtil.get("reset-password:" + token)).thenReturn(accountId);
        assertEquals(accountId, accountTokenService.getAccountIdByResetPasswordToken(token));
    }

    @Test
    void deleteResetPasswordToken_Success() {
        accountTokenService.deleteResetPasswordToken(token);
        verify(redisUtil).delete("reset-password:" + token);
    }
}
