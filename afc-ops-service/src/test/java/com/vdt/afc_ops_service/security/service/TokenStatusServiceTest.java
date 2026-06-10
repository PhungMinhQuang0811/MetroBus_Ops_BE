package com.vdt.afc_ops_service.security.service;

import com.vdt.afc_ops_service.constant.SecurityConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenStatusServiceTest {

    @Mock
    RedisTemplate<String, String> redisTemplate;

    @Test
    void isAccessTokenInvalidated_ChecksExpectedKey() {
        TokenStatusService service = new TokenStatusService(redisTemplate);
        when(redisTemplate.hasKey("accessTk:token-1")).thenReturn(true);

        assertTrue(service.isAccessTokenInvalidated("token-1"));
    }

    @Test
    void isAccountDisabled_ChecksExpectedKey() {
        TokenStatusService service = new TokenStatusService(redisTemplate);
        when(redisTemplate.hasKey(SecurityConstants.ACCOUNT_DISABLED_KEY_PREFIX + "account-1")).thenReturn(false);

        assertFalse(service.isAccountDisabled("account-1"));
    }
}
