package com.example.student.common;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 统一缓存/限流抽象：Redis 可用时走 Redis（多实例一致），
 * 不可用时自动降级为进程内实现（单实例安全，多实例各自为政但功能不中断）。
 *
 * 阶段六：解决此前"内存缓存/限流为单实例方案"的多实例部署短板。
 */
@Component
public class DistributedCacheService {

    private static final String KEY_PREFIX = "sm:cache:";

    private final StringRedisTemplate redis;
    private final ConcurrentHashMap<String, CacheEntry> localCache = new ConcurrentHashMap<>();

    private record CacheEntry(Object value, long expiresAt) {
        boolean fresh() { return System.currentTimeMillis() < expiresAt; }
    }

    public DistributedCacheService(@org.springframework.beans.factory.annotation.Autowired(required = false)
                                   StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean isRedisBacked() {
        return redis != null;
    }

    /** 读穿缓存：loader 计算真实值。ttlSeconds 控制新鲜度窗口 */
    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, long ttlSeconds, Function<String, T> loader) {
        if (isRedisBacked()) {
            try {
                String cached = redis.opsForValue().get(KEY_PREFIX + key);
                if (cached != null) {
                    return (T) deserialize(cached);
                }
                T value = loader.apply(key);
                if (value != null) {
                    redis.opsForValue().set(KEY_PREFIX + key, serialize(value), Duration.ofSeconds(ttlSeconds));
                }
                return value;
            } catch (Exception ignored) {
                // Redis 故障降级到本地
            }
        }
        // 本地降级路径
        CacheEntry e = localCache.get(key);
        if (e != null && e.fresh()) return (T) e.value();
        T value = loader.apply(key);
        localCache.put(key, new CacheEntry(value, System.currentTimeMillis() + ttlSeconds * 1000));
        return value;
    }

    /** 限流尝试：滑动窗口计数。返回 true=放行 */
    public boolean tryAcquire(String bucketKey, int limitPerMinute) {
        if (isRedisBacked()) {
            try {
                Long count = redis.opsForValue().increment(KEY_PREFIX + "rate:" + bucketKey);
                if (count != null && count == 1L) {
                    redis.expire(KEY_PREFIX + "rate:" + bucketKey, Duration.ofMinutes(1));
                }
                return count != null && count <= limitPerMinute;
            } catch (Exception ignored) {
                // 降级本地
            }
        }
        long window = System.currentTimeMillis() / 60_000L;
        String localKey = bucketKey + ":" + window;
        int[] counter = {0};
        localCache.compute(localKey, (k, v) -> new CacheEntry(new int[]{(v == null ? 0 : ((int[]) v.value())[0]) + 1}, (window + 1) * 60_000));
        Object v = localCache.get(localKey).value();
        return ((int[]) v)[0] <= limitPerMinute;
    }

    /** 自增计数器（登录失败计数等），首次自增时设置过期窗口 */
    public Long incrementCounter(String key, long ttlSeconds) {
        if (isRedisBacked()) {
            try {
                Long count = redis.opsForValue().increment(KEY_PREFIX + key);
                if (count != null && count == 1L) {
                    redis.expire(KEY_PREFIX + key, Duration.ofSeconds(ttlSeconds));
                }
                return count;
            } catch (Exception ignored) {
                // Redis 故障降级到本地
            }
        }
        long window = System.currentTimeMillis() / (ttlSeconds * 1000L);
        String localKey = key + ":" + window;
        int[] updated = new int[1];
        localCache.compute(localKey, (k, v) -> {
            int prev = 0;
            if (v instanceof CacheEntry) {
                Object inner = ((CacheEntry) v).value();
                if (inner instanceof int[]) {
                    prev = ((int[]) inner)[0];
                }
            }
            updated[0] = prev + 1;
            return new CacheEntry(updated, (window + 1) * ttlSeconds * 1000L);
        });
        return (long) updated[0];
    }

    /** 仅当键不存在时写入（锁定标记等），返回 true=写入成功 */
    public boolean putIfAbsent(String key, Object value, long ttlSeconds) {
        if (isRedisBacked()) {
            try {
                Boolean ok = redis.opsForValue().setIfAbsent(KEY_PREFIX + key,
                        serialize(value), Duration.ofSeconds(ttlSeconds));
                return Boolean.TRUE.equals(ok);
            } catch (Exception ignored) {
                // Redis 故障降级到本地
            }
        }
        CacheEntry e = localCache.get(key);
        if (e != null && e.fresh()) return false;
        localCache.put(key, new CacheEntry(value, System.currentTimeMillis() + ttlSeconds * 1000));
        return true;
    }

    /** 权限变更时主动清除该用户相关缓存（跨实例生效需 Redis） */
    public void evictByPrefix(String keyPrefix) {
        if (isRedisBacked()) {
            try {
                var keys = redis.keys(KEY_PREFIX + keyPrefix + "*");
                if (keys != null && !keys.isEmpty()) redis.delete(keys);
            } catch (Exception ignored) { }
        }
        localCache.keySet().removeIf(k -> k.startsWith(keyPrefix));
    }

    // 简易序列化：数字与字符串直接存，其余 JSON；序列化失败抛错（不存垃圾 toString）
    private String serialize(Object v) {
        if (v instanceof Number || v instanceof String || v instanceof Boolean) return String.valueOf(v);
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(v);
        } catch (Exception e) {
            throw new IllegalStateException("cache serialize failed: " + e.getMessage(), e);
        }
    }

    private Object deserialize(String s) {
        try {
            if (s.startsWith("{") || s.startsWith("[")) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(s, Object.class);
            }
            if (s.matches("-?\\d+")) return Long.parseLong(s);
            return s;
        } catch (Exception e) {
            return s;
        }
    }
}
