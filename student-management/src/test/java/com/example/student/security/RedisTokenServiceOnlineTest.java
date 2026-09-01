package com.example.student.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** M1-3 在线会话测试：心跳写入、列表解析、强退拉黑。 */
class RedisTokenServiceOnlineTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> ops;
    private RedisTokenService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        service = new RedisTokenService(redisTemplate);
    }

    @Test
    void touchOnlineWritesWithTtl() {
        service.touchOnline(1L, "jti-abc", "zhangsan", "张三");

        verify(ops).set(eq("online:1:jti-abc"), eq("zhangsan|张三"), eq(30 * 60L), eq(TimeUnit.SECONDS));
    }

    @Test
    void touchOnlineBlankRealNameDegrades() {
        service.touchOnline(1L, "jti-abc", "zhangsan", null);

        verify(ops).set(eq("online:1:jti-abc"), eq("zhangsan|"), anyLong(), any(TimeUnit.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listOnlineParsesKeysAndValue() {
        when(redisTemplate.keys("online:*")).thenReturn(Set.of("online:1:jti-abc", "online:2:jti-def"));
        when(ops.get("online:1:jti-abc")).thenReturn("zhangsan|张三");
        when(ops.get("online:2:jti-def")).thenReturn("lisi|李四");
        when(redisTemplate.getExpire("online:1:jti-abc", TimeUnit.SECONDS)).thenReturn(120L);
        when(redisTemplate.getExpire("online:2:jti-def", TimeUnit.SECONDS)).thenReturn(300L);

        List<Map<String, Object>> list = service.listOnline();

        assertEquals(2, list.size());
        // 按剩余 TTL 降序：lisi(300) 在前
        assertEquals("2", String.valueOf(list.get(0).get("userId")));
        assertEquals("lisi", list.get(0).get("username"));
        assertEquals("李四", list.get(0).get("realName"));
        assertEquals("jti-def", list.get(0).get("jti"));
        assertEquals("1", String.valueOf(list.get(1).get("userId")));
    }

    @Test
    void removeOnlineDeletesKey() {
        service.removeOnline(1L, "jti-abc");

        verify(redisTemplate).delete("online:1:jti-abc");
    }

    @Test
    void forceLogoutRemovesAndBlacklists() {
        service.forceLogout(1L, "jti-abc", 600);

        verify(redisTemplate).delete("online:1:jti-abc");
        verify(ops).set(eq("jwt:blacklist:jti-abc"), eq("1"), eq(600L), eq(TimeUnit.SECONDS));
    }
}