package com.example.student.audit;

import com.example.student.common.DistributedCacheService;
import com.example.student.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/** M1-2 限流切面：读取 @RateLimit，复用 DistributedCacheService.tryAcquire 做每分钟计数。 */
@Aspect
@Component
public class RateLimitAspect {

    private final DistributedCacheService cache;
    private final HttpServletRequest request;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer names = new DefaultParameterNameDiscoverer();

    public RateLimitAspect(DistributedCacheService cache, HttpServletRequest request) {
        this.cache = cache;
        this.request = request;
    }

    @Around("@annotation(rateLimit)")
    public Object limit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 同时支持重复标注：@RateLimits 容器里的全部生效
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RateLimit[] all = method.getAnnotationsByType(RateLimit.class);
        if (all.length == 0) all = new RateLimit[]{rateLimit};
        for (RateLimit r : all) {
            String suffix = resolveSuffix(joinPoint, r);
            String bucketKey = r.bucket() + ":" + suffix;
            if (!cache.tryAcquire(bucketKey, r.limit())) {
                throw new RateLimitException(r.message());
            }
        }
        return joinPoint.proceed();
    }

    /** 维度 key：注解给了 SpEL 用参数解析（如 #loginDTO.username），否则按客户端 IP */
    private String resolveSuffix(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String expr = rateLimit.key();
        if (expr != null && !expr.isBlank()) {
            MethodSignature sig = (MethodSignature) joinPoint.getSignature();
            Method method = sig.getMethod();
            Object[] args = joinPoint.getArgs();
            String[] paramNames = names.getParameterNames(method);
            if (paramNames != null && paramNames.length == args.length) {
                StandardEvaluationContext ctx = new StandardEvaluationContext();
                for (int i = 0; i < paramNames.length; i++) {
                    if (args[i] != null) ctx.setVariable(paramNames[i], args[i]);
                }
                try {
                    Object v = parser.parseExpression(expr).getValue(ctx);
                    if (v != null) return String.valueOf(v);
                } catch (Exception ignored) {
                    // SpEL 解析失败降级 IP
                }
            }
        }
        String ip = request.getRemoteAddr();
        return ip == null ? "unknown" : ip;
    }
}