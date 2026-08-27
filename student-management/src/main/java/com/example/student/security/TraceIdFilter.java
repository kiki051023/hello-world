package com.example.student.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * traceId 日志链路过滤器（通过 FilterRegistrationBean 注册为最高优先级，先于 Spring Security 过滤链）：
 * <ul>
 *   <li>请求头 {@code X-Trace-Id} 有值则沿用（网关透传）；否则回退读取 {@code X-Request-Id}（兼容旧调用方）；再兜底生成短 traceId</li>
 *   <li>{@code MDC.put("traceId")} 供日志 pattern {@code [%X{traceId:-}]} 输出，审计切面复用同一值写入 operation_log.request_id</li>
 *   <li>响应头 {@code X-Trace-Id} 回写，供调用方串联全链路</li>
 *   <li>{@code finally} 中 {@code MDC.remove}，防止线程池复用导致串号</li>
 * </ul>
 */
public class TraceIdFilter extends OncePerRequestFilter {

    /** MDC key，logback pattern 与审计切面复用 */
    public static final String MDC_KEY = "traceId";
    /** 首选透传头 */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    /** 兼容旧调用方（审计切面原读取头） */
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(HEADER_TRACE_ID);
        if (!StringUtils.hasText(traceId)) {
            traceId = request.getHeader(HEADER_REQUEST_ID);
        }
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        }
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER_TRACE_ID, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /** 当前线程请求的 traceId（无则返回 null），供切面/业务复用保证与日志一致 */
    public static String currentTraceId() {
        return MDC.get(MDC_KEY);
    }
}