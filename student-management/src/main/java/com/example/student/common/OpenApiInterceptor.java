package com.example.student.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.student.mapper.ApiIdempotencyMapper;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 阶段六开放接口适配层：/api/v1/** 的幂等键 + 滑动窗口限流 + 交换审计。
 *
 * - 写操作必须携带 Idempotency-Key；重复请求回放首次响应（409 冲突时拒绝）
 * - 每个 client_id 每分钟最多 120 次，超限 429
 * - 全部交换写入 api_exchange_audit
 */
@Component
public class OpenApiInterceptor implements HandlerInterceptor {

    static final int RATE_LIMIT_PER_MINUTE = 120;
    private static final long IDEMPOTENCY_TTL_HOURS = 24;

    @Autowired
    private ApiIdempotencyMapper idempotencyMapper;

    // client_id -> 时间戳队列（滑动窗口）
    private final Map<String, Deque<Long>> rateBuckets = new ConcurrentHashMap<>();

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String clientId = request.getHeader("X-Client-Id");
        if (clientId == null || clientId.isBlank()) clientId = "anonymous";
        String requestId = request.getHeader("X-Request-Id");
        String path = request.getRequestURI();
        String method = request.getMethod();

        // ---- 限流（滑动窗口）----
        if (!allowRate(clientId)) {
            audit(clientId, method, path, null, 429, "RATE_LIMITED", requestId);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后重试\"}");
            return false;
        }

        // ---- 幂等键（仅写操作）----
        String idemKey = request.getHeader("Idempotency-Key");
        boolean isWrite = !method.equalsIgnoreCase(HttpMethod.GET.name())
                && !method.equalsIgnoreCase(HttpMethod.OPTIONS.name());
        if (isWrite) {
            if (idemKey == null || idemKey.isBlank()) {
                audit(clientId, method, path, null, 400, "MISSING_IDEMPOTENCY_KEY", requestId);
                response.setStatus(400);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":400,\"message\":\"写操作必须携带 Idempotency-Key\"}");
                return false;
            }
            String fingerprint = method + " " + path;
            ApiIdempotency existing = idempotencyMapper.selectOne(new LambdaQueryWrapper<ApiIdempotency>()
                    .eq(ApiIdempotency::getIdempotencyKey, idemKey)
                    .eq(ApiIdempotency::getRequestFingerprint, fingerprint)
                    .gt(ApiIdempotency::getExpiresAt, LocalDateTime.now()));
            if (existing != null) {
                audit(clientId, method, path, idemKey,
                        existing.getHttpStatus() != null ? existing.getHttpStatus() : 200, "REPLAYED", requestId);
                response.setStatus(existing.getHttpStatus() != null ? existing.getHttpStatus() : 200);
                response.setContentType("application/json;charset=UTF-8");
                response.setHeader("Idempotent-Replayed", "true");
                response.getWriter().write(existing.getResponseBody() == null ? "{}" : existing.getResponseBody());
                return false;
            }
            // 登记占位（首次执行）；完成后由 Controller advice 回填响应体
            ApiIdempotency rec = new ApiIdempotency();
            rec.setIdempotencyKey(idemKey);
            rec.setRequestFingerprint(fingerprint);
            rec.setCreatedAt(LocalDateTime.now());
            rec.setExpiresAt(LocalDateTime.now().plusHours(IDEMPOTENCY_TTL_HOURS));
            try {
                idempotencyMapper.insert(rec);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 并发同键：按冲突处理
                audit(clientId, method, path, idemKey, 409, "IDEMPOTENT_CONFLICT", requestId);
                response.setStatus(409);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":409,\"message\":\"相同 Idempotency-Key 正在处理中\"}");
                return false;
            }
        }

        audit(clientId, method, path, idemKey, null, null, requestId);
        return true;
    }

    /** 完成后由 Controller 调用：登记响应供后续幂等回放 */
    public void recordResponse(HttpServletRequest request, int status, String bodyJson) {
        String idemKey = request.getHeader("Idempotency-Key");
        if (idemKey == null || idemKey.isBlank()) return;
        String fingerprint = request.getMethod() + " " + request.getRequestURI();
        ApiIdempotency rec = idempotencyMapper.selectOne(new LambdaQueryWrapper<ApiIdempotency>()
                .eq(ApiIdempotency::getIdempotencyKey, idemKey)
                .eq(ApiIdempotency::getRequestFingerprint, fingerprint)
                .gt(ApiIdempotency::getExpiresAt, LocalDateTime.now()));
        if (rec != null) {
            rec.setHttpStatus(status);
            rec.setResponseBody(bodyJson != null && bodyJson.length() > 60000
                    ? bodyJson.substring(0, 60000) : bodyJson);
            idempotencyMapper.updateById(rec);
        }
    }

    private synchronized boolean allowRate(String clientId) {
        long now = System.currentTimeMillis();
        Deque<Long> q = rateBuckets.computeIfAbsent(clientId, k -> new ArrayDeque<>());
        while (!q.isEmpty() && now - q.peekFirst() > 60_000L) q.pollFirst();
        if (q.size() >= RATE_LIMIT_PER_MINUTE) return false;
        q.addLast(now);
        return true;
    }

    private void audit(String clientId, String method, String path, String idemKey,
                       Integer status, String rejectedReason, String requestId) {
        try {
            var e = new com.example.student.common.ApiExchangeAudit();
            e.setClientId(truncate(clientId, 64));
            e.setApiVersion(com.example.student.config.ApiVersionConfig.API_VERSION);
            e.setRequestMethod(method);
            e.setRequestPath(truncate(path, 255));
            e.setIdempotencyKey(idemKey == null ? null : truncate(idemKey, 128));
            e.setHttpStatus(status);
            e.setRejectedReason(rejectedReason);
            e.setRequestId(requestId == null ? null : truncate(requestId, 64));
            // 直接 SQL 插入避免循环依赖审计 mapper 未建
            idempotencyMapper.insertAudit(e);
        } catch (Exception ignored) {
            // 审计失败不阻塞主流程
        }
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    @PreDestroy
    void cleanup() {
        rateBuckets.clear();
    }
}
