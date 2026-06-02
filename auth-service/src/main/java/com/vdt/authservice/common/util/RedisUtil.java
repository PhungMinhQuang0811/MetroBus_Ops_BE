package com.vdt.authservice.common.util;

import com.vdt.authservice.config.RedisConfig;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisUtil {
    private static final String OTP_KEY_PREFIX = "auth:otp:phone:";
    private static final String OTP_COOLDOWN_KEY_PREFIX = "auth:otp:cooldown:phone:";
    private static final String OTP_PHONE_RATE_LIMIT_KEY_PREFIX = "auth:otp:rate:phone:day:";
    private static final String OTP_VERIFY_ATTEMPT_KEY_PREFIX = "auth:otp:verify-attempt:phone:";

    RedisTemplate<String, String> redisTemplate;

    public void set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public void expire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }

    public Long getExpire(String key, TimeUnit unit) {
        return redisTemplate.getExpire(key, unit);
    }

    public void addSet(String key, java.util.Collection<String> values, long timeout, TimeUnit unit) {
        if (values != null && !values.isEmpty()) {
            redisTemplate.opsForSet().add(key, values.toArray(new String[0]));
            redisTemplate.expire(key, timeout, unit);
        }
    }

    public Set<String> getSet(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public String buildOtpKey(String phoneNumber) {
        return OTP_KEY_PREFIX + phoneNumber;
    }

    public String buildOtpCooldownKey(String phoneNumber) {
        return OTP_COOLDOWN_KEY_PREFIX + phoneNumber;
    }

    public String buildOtpPhoneRateLimitKey(String phoneNumber) {
        return OTP_PHONE_RATE_LIMIT_KEY_PREFIX + phoneNumber;
    }

    public String buildOtpVerifyAttemptKey(String phoneNumber) {
        return OTP_VERIFY_ATTEMPT_KEY_PREFIX + phoneNumber;
    }

    public void deleteByPrefix(String prefix) {
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
