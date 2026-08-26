package com.example.student.security;

import com.example.student.common.DistributedCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/** 登录失败锁定：5 次失败触发锁定，成功登录清零，锁定期间拒绝尝试。 */
class LoginLockoutServiceTest {

    private DistributedCacheService cache;
    private LoginLockoutService service;

    @BeforeEach
    void setUp() {
        cache = mock(DistributedCacheService.class);
        service = new LoginLockoutService(cache);
    }

    @Test
    void lockedWhenReachingMaxFailures() {
        when(cache.incrementCounter(anyString(), anyLong())).thenReturn(1L, 2L, 3L, 4L, 5L);
        when(cache.putIfAbsent(anyString(), any(), anyLong())).thenReturn(true);

        for (int i = 0; i < 4; i++) {
            service.recordFailure("alice");
        }
        verify(cache, never()).putIfAbsent(anyString(), any(), anyLong());

        service.recordFailure("alice"); // 第 5 次
        verify(cache).putIfAbsent(contains("locked:alice"), eq("1"), anyLong());
    }

    @Test
    void notLockedWhenNoLockMark() {
        when(cache.getOrLoad(anyString(), anyLong(), any())).thenReturn(null);
        assertFalse(service.isLocked("bob"));
    }

    @Test
    void lockedCheckReadsLockKey() {
        when(cache.getOrLoad(anyString(), anyLong(), any())).thenReturn("1");
        assertTrue(service.isLocked("alice"));
    }

    @Test
    void successResetsCounter() {
        service.resetFailures("alice");
        // 修正后：同时清除计数键(login:fail:alice)与锁定键(login:fail:locked:alice)
        // 两者都含 "alice"，需用互斥谓词分别断言，否则 contains("alice") 会同时命中两次。
        verify(cache).evictByPrefix(argThat(s -> s != null && s.toString().contains("alice") && !s.toString().contains("locked:")));
        verify(cache).evictByPrefix(contains("locked:alice"));
    }

    @Test
    void blankUsernameIsIgnored() {
        service.recordFailure("");
        service.recordFailure(null);
        verify(cache, never()).incrementCounter(anyString(), anyLong());
    }

    /** 真实（进程内）缓存走完整链路：5 次失败→锁定→成功清零。 */
    @Test
    void fullFlowWithRealLocalCache() {
        DistributedCacheService real = new DistributedCacheService(null);
        LoginLockoutService svc = new LoginLockoutService(real);
        for (int i = 0; i < 5; i++) {
            svc.recordFailure("carol");
        }
        assertTrue(svc.isLocked("carol"));
        svc.resetFailures("carol");
        assertFalse(svc.isLocked("carol"));
    }

    /**
     * 回归测试：Redis 路径下 deserialize 会把锁标记 "1" 还原为 Long，
     * 旧实现用 String 接收 getOrLoad 结果会抛 ClassCastException（线上 500）。
     * 锁定判定必须按“非空即锁定”，与返回类型无关。
     */
    @Test
    void lockedCheckHandlesRedisLongToken() {
        when(cache.getOrLoad(anyString(), anyLong(), any())).thenReturn(Long.valueOf(1));
        assertTrue(service.isLocked("alice"));
    }
}
