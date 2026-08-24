package com.example.student.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的令牌状态管理：
 * - 登出/吊销的访问令牌进入黑名单（TTL = 令牌剩余有效期）
 * - 刷新令牌的 jti 按用户维度存储（TTL = 刷新令牌有效期），用于登出吊销与刷新校验
 */
@Component
public class RedisTokenService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String REFRESH_PREFIX = "jwt:refresh:";

    private final StringRedisTemplate redisTemplate;

    public RedisTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 将访问令牌加入黑名单，TTL 到期后自动清除 */
    public void blacklistAccessToken(String jti, long ttlSeconds) {
        if (jti == null || ttlSeconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "1", ttlSeconds, TimeUnit.SECONDS);
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }

    /** 记录用户当前有效的刷新令牌 jti */
    public void storeRefreshToken(Long userId, String jti, long ttlSeconds) {
        if (userId == null || jti == null) {
            return;
        }
        redisTemplate.opsForValue().set(REFRESH_PREFIX + userId, jti, ttlSeconds, TimeUnit.SECONDS);
    }

    public String getRefreshTokenJti(Long userId) {
        if (userId == null) {
            return null;
        }
        return redisTemplate.opsForValue().get(REFRESH_PREFIX + userId);
    }

    /** 删除用户的刷新令牌（登出时调用，使其立即失效） */
    public void removeRefreshToken(Long userId) {
        if (userId == null) {
            return;
        }
        redisTemplate.delete(REFRESH_PREFIX + userId);
    }
}
