package com.example.student.audit;

import com.example.student.common.DistributedCacheService;
import com.example.student.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/** M1-2 限流切面单元测试：单层/重复注解、SpEL key、超限抛 RateLimitException。 */
class RateLimitAspectTest {

    private DistributedCacheService cache;
    private HttpServletRequest request;
    private RateLimitAspect aspect;
    private MethodSignature signature;
    private ProceedingJoinPoint joinPoint;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        cache = mock(DistributedCacheService.class);
        request = mock(HttpServletRequest.class);
        aspect = new RateLimitAspect(cache, request);
        joinPoint = mock(ProceedingJoinPoint.class);
        signature = mock(MethodSignature.class);
        Method m = TestController.class.getMethod("login", TestDTO.class);
        when(signature.getMethod()).thenReturn(m);
        when(joinPoint.getSignature()).thenReturn(signature);
    }

    static class TestDTO {
        public String username;
        public TestDTO(String username) { this.username = username; }
    }

    static class TestController {
        @RateLimit(bucket = "auth:login:ip", limit = 10)
        @RateLimit(bucket = "auth:login:user", key = "#dto.username", limit = 5)
        public Object login(TestDTO dto) { return "ok"; }
    }

    @Test
    void repeatedAnnotationsAllCheckedAndSpelKeyResolves() throws Throwable {
        when(cache.tryAcquire("auth:login:ip:127.0.0.1", 10)).thenReturn(true);
        when(cache.tryAcquire("auth:login:user:zhangsan", 5)).thenReturn(true);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(joinPoint.getArgs()).thenReturn(new Object[]{new TestDTO("zhangsan")});
        when(joinPoint.proceed()).thenReturn("ok");

        Object r = aspect.limit(joinPoint, signature.getMethod().getAnnotationsByType(RateLimit.class)[0]);

        verify(cache).tryAcquire("auth:login:ip:127.0.0.1", 10);
        verify(cache).tryAcquire("auth:login:user:zhangsan", 5);
        verify(joinPoint).proceed();
    }

    @Test
    void throwsRateLimitWhenAnyBucketExceeded() throws Throwable {
        when(cache.tryAcquire("auth:login:ip:127.0.0.1", 10)).thenReturn(true);
        when(cache.tryAcquire("auth:login:user:zhangsan", 5)).thenReturn(false); // 超限
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(joinPoint.getArgs()).thenReturn(new Object[]{new TestDTO("zhangsan")});

        assertThrows(RateLimitException.class,
                () -> aspect.limit(joinPoint, signature.getMethod().getAnnotationsByType(RateLimit.class)[0]));
        verify(joinPoint, never()).proceed();
    }
}