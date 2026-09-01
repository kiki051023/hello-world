package com.example.student.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
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
    private static final String PERMISSION_VERSION_PREFIX = "auth:permission-version:";
    /** 在线会话：online:{userId}:{jti}，TTL 即活跃窗口（30 分钟），每次请求刷新 */
    private static final String ONLINE_PREFIX = "online:";
    private static final long ONLINE_TTL_SECONDS = 30 * 60;

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

    public void storePermissionVersion(Long userId, Integer version) {
        if (userId != null && version != null) {
            redisTemplate.opsForValue().set(PERMISSION_VERSION_PREFIX + userId, version.toString());
        }
    }

    public Integer getPermissionVersion(Long userId) {
        if (userId == null) return null;
        String value = redisTemplate.opsForValue().get(PERMISSION_VERSION_PREFIX + userId);
        return value == null ? null : Integer.valueOf(value);
    }

    /**
     * 记录/心跳在线会话。TTL 即活跃窗口，每次鉴权请求刷新，天然实现过期下线。
     * value 存 username:realName 便于管理端展示（格式 username|realName）。
     */
    public void touchOnline(Long userId, String jti, String username, String realName) {
        if (userId == null || jti == null) return;
        String safeReal = (realName == null || realName.isBlank()) ? "" : realName;
        redisTemplate.opsForValue().set(ONLINE_PREFIX + userId + ":" + jti,
                username + "|" + safeReal, ONLINE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /** 登出/强退时移除指定会话 */
    public void removeOnline(Long userId, String jti) {
        if (userId == null || jti == null) return;
        redisTemplate.delete(ONLINE_PREFIX + userId + ":" + jti);
    }

    /** 列出全部在线会话（扫描 online:*），返回 List<Map{userId,jti,username,realName,lastActive}> */
    public List<Map<String, Object>> listOnline() {
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        try {
            var keys = redisTemplate.keys(ONLINE_PREFIX + "*");
            if (keys == null || keys.isEmpty()) return result;
            for (String key : keys) {
                String value = redisTemplate.opsForValue().get(key);
                if (value == null) continue;
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                // key: online:{userId}:{jti}
                int first = key.indexOf(':', ONLINE_PREFIX.length());
                if (first < 0) continue;
                Map<String, Object> row = new java.util.HashMap<>();
                row.put("userId", Long.valueOf(key.substring(ONLINE_PREFIX.length(), first)));
                row.put("jti", key.substring(first + 1));
                String[] parts = value.split("\\|", -1);
                row.put("username", parts.length > 0 ? parts[0] : "");
                row.put("realName", parts.length > 1 ? parts[1] : "");
                row.put("lastActiveSeconds", ttl); // 剩余 TTL，越大越活跃
                result.add(row);
            }
            result.sort((a, b) -> Long.compare(
                    (Long) b.get("lastActiveSeconds"), (Long) a.get("lastActiveSeconds")));
        } catch (Exception ignored) {
            // Redis 故障时在线列表返回空，不影响其它功能
        }
        return result;
    }

    /** 强退：删除在线会话并拉黑该访问令牌（若未过期），使请求立即失效 */
    public void forceLogout(Long userId, String jti, long accessTokenTtlSeconds) {
        removeOnline(userId, jti);
        if (jti != null && accessTokenTtlSeconds > 0) {
            blacklistAccessToken(jti, accessTokenTtlSeconds);
        }
    }
}
