package com.example.student.common;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/** DistributedCacheService 本地降级路径：读穿缓存、计数器、锁定标记。 */
class DistributedCacheServiceTest {

    /** 无 Redis 构造（redis=null），强制走本地降级路径 */
    private final DistributedCacheService cache = new DistributedCacheService(null);

    @Test
    void localFallbackLoadsOnceWithinTtl() {
        AtomicInteger loads = new AtomicInteger();
        Function<String, String> loader = k -> {
            loads.incrementAndGet();
            return "v-" + loads.get();
        };

        assertEquals("v-1", cache.getOrLoad("k", 60, loader));
        assertEquals("v-1", cache.getOrLoad("k", 60, loader)); // TTL 内命中本地缓存
        assertEquals(1, loads.get());
    }

    @Test
    void incrementCounterCountsUp() {
        assertEquals(1L, cache.incrementCounter("fail:u1", 60));
        assertEquals(2L, cache.incrementCounter("fail:u1", 60));
        assertEquals(3L, cache.incrementCounter("fail:u1", 60));
        assertEquals(1L, cache.incrementCounter("fail:u2", 60)); // 不同键独立
    }

    @Test
    void putIfAbsentOnlyFirstWins() {
        assertTrue(cache.putIfAbsent("locked:u1", "1", 60));
        assertFalse(cache.putIfAbsent("locked:u1", "1", 60)); // 已存在
    }

    @Test
    void evictByPrefixRemovesLocalEntries() {
        cache.getOrLoad("overview:u=1", 60, k -> "x");
        cache.evictByPrefix("overview:");
        // 清除后重新加载：loader 会再次执行（返回值不同可证明）
        String v = cache.getOrLoad("overview:u=1", 60, k -> "y");
        assertEquals("y", v);
    }

    @Test
    void isRedisBackedFalseWithoutRedis() {
        assertFalse(cache.isRedisBacked());
    }
}
