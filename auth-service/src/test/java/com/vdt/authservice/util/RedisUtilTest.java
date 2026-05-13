package com.vdt.authservice.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisUtilTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private RedisUtil redisUtil;

    private final String key = "test-key";
    private final String value = "test-value";

    @Test
    void set_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        redisUtil.set(key, value, 1, TimeUnit.HOURS);
        verify(valueOperations).set(key, value, 1, TimeUnit.HOURS);
    }

    @Test
    void get_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(value);
        assertEquals(value, redisUtil.get(key));
    }

    @Test
    void addSet_Success() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        java.util.List<String> values = java.util.List.of("v1", "v2");
        redisUtil.addSet(key, values, 1, TimeUnit.HOURS);
        
        // Ép kiểu (String[]) any() để khớp với Varargs được truyền dưới dạng mảng
        verify(setOperations).add(eq(key), any(String[].class));
        verify(redisTemplate).expire(key, 1, TimeUnit.HOURS);
    }

    @Test
    void getSet_Success() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        Set<String> values = Set.of("v1", "v2");
        when(setOperations.members(key)).thenReturn(values);
        assertEquals(values, redisUtil.getSet(key));
    }

    @Test
    void delete_Success() {
        redisUtil.delete(key);
        verify(redisTemplate).delete(key);
    }

    @Test
    void hasKey_ReturnsTrue() {
        when(redisTemplate.hasKey(key)).thenReturn(true);
        assertTrue(redisUtil.hasKey(key));
    }

    @Test
    void addSet_NullValues_DoesNothing() {
        redisUtil.addSet(key, null, 1, TimeUnit.HOURS);
        verify(redisTemplate, never()).opsForSet();
    }

    @Test
    void addSet_EmptyValues_DoesNothing() {
        redisUtil.addSet(key, java.util.Collections.emptyList(), 1, TimeUnit.HOURS);
        verify(redisTemplate, never()).opsForSet();
    }

    @Test
    void deleteByPrefix_Success() {
        String prefix = "user_perms:";
        Set<String> keys = Set.of("user_perms:1", "user_perms:2");
        when(redisTemplate.keys(prefix + "*")).thenReturn(keys);

        redisUtil.deleteByPrefix(prefix);

        verify(redisTemplate).delete(keys);
    }

    @Test
    void deleteByPrefix_KeysNull_ShouldNotDelete() {
        String prefix = "empty:";
        when(redisTemplate.keys(prefix + "*")).thenReturn(null);

        redisUtil.deleteByPrefix(prefix);

        verify(redisTemplate, never()).delete(anyCollection());
    }

    @Test
    void deleteByPrefix_KeysEmpty_ShouldNotDelete() {
        String prefix = "empty:";
        when(redisTemplate.keys(prefix + "*")).thenReturn(java.util.Collections.emptySet());

        redisUtil.deleteByPrefix(prefix);

        verify(redisTemplate, never()).delete(anyCollection());
    }
}
