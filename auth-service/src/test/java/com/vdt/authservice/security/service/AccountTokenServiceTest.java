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
    void generateVerificationToken_Success() {
        String result = accountTokenService.generateVerificationToken(accountId);
        assertNotNull(result);
        verify(redisUtil, times(2)).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void getExistingVerificationToken_Success() {
        when(redisUtil.get("verification_user:" + accountId)).thenReturn(token);
        assertEquals(token, accountTokenService.getExistingVerificationToken(accountId));
    }

    @Test
    void getAccountIdByVerificationToken_Success() {
        when(redisUtil.get("verification:" + token)).thenReturn(accountId);
        assertEquals(accountId, accountTokenService.getAccountIdByVerificationToken(token));
    }

    @Test
    void deleteVerificationToken_Success() {
        when(redisUtil.get("verification:" + token)).thenReturn(accountId);
        accountTokenService.deleteVerificationToken(token);
        verify(redisUtil).delete("verification_user:" + accountId);
        verify(redisUtil).delete("verification:" + token);
    }

    @Test
    void deleteVerificationToken_NotFound_StillDeletesToken() {
        when(redisUtil.get("verification:" + token)).thenReturn(null);
        accountTokenService.deleteVerificationToken(token);
        verify(redisUtil, never()).delete(startsWith("verification_user:"));
        verify(redisUtil).delete("verification:" + token);
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
