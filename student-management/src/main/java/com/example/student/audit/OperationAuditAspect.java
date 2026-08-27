package com.example.student.audit;

import com.example.student.security.AuthorizationService;
import com.example.student.security.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
public class OperationAuditAspect {
    private final OperationLogService logService;
    private final AuthorizationService authorizationService;
    private final HttpServletRequest request;

    public OperationAuditAspect(OperationLogService logService, AuthorizationService authorizationService,
                                HttpServletRequest request) {
        this.logService = logService;
        this.authorizationService = authorizationService;
        this.request = request;
    }

    @Around("execution(public * com.example.student.controller..*(..))")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        OperationLog log = new OperationLog();
        log.setAction(joinPoint.getSignature().getName());
        log.setRequestMethod(request.getMethod());
        log.setRequestPath(request.getRequestURI());
        // request_id 优先取 MDC traceId（与日志链路一致），其次兼容旧调用方 X-Request-Id 头
        String traceId = TraceIdFilter.currentTraceId();
        log.setRequestId(traceId != null ? traceId : request.getHeader("X-Request-Id"));
        log.setClientIp(request.getRemoteAddr());
        try {
            var user = authorizationService.currentUser();
            log.setOperatorId(user.getId());
            log.setUsername(user.getUsername());
        } catch (RuntimeException ignored) {
            log.setUsername("anonymous");
        }
        try {
            Object result = joinPoint.proceed();
            log.setResult("SUCCESS");
            return result;
        } catch (Throwable ex) {
            log.setResult("FAILURE");
            log.setErrorMessage(ex.getClass().getSimpleName() + ":" + ex.getMessage());
            throw ex;
        } finally {
            if (log.getRequestId() == null) log.setRequestId(UUID.randomUUID().toString());
            logService.record(log);
        }
    }
}
