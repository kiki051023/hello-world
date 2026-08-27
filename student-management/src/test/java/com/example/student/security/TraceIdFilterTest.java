package com.example.student.security;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void generatesTraceIdWhenNoHeaderPresent() throws Exception {
        AtomicReference<String> mdcInside = new AtomicReference<>();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/system/health"), response,
                (req, res) -> mdcInside.set(MDC.get(TraceIdFilter.MDC_KEY)));

        String traceId = response.getHeader(TraceIdFilter.HEADER_TRACE_ID);
        assertThat(traceId).isNotBlank();
        assertThat(traceId.length()).isEqualTo(8);
        // 过滤器执行期间 MDC 已写入，且与响应头一致
        assertThat(mdcInside.get()).isEqualTo(traceId);
        // finally 已清理，避免线程池复用串号
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void reusesIncomingXTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/system/health");
        request.addHeader(TraceIdFilter.HEADER_TRACE_ID, "client-trace-001");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
        });

        assertThat(response.getHeader(TraceIdFilter.HEADER_TRACE_ID)).isEqualTo("client-trace-001");
    }

    @Test
    void fallsBackToXRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/system/health");
        request.addHeader(TraceIdFilter.HEADER_REQUEST_ID, "legacy-req-42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
        });

        assertThat(response.getHeader(TraceIdFilter.HEADER_TRACE_ID)).isEqualTo("legacy-req-42");
    }

    @Test
    void cleansMdcEvenWhenChainThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/system/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            filter.doFilter(request, response, (req, res) -> {
                throw new RuntimeException("boom");
            });
        } catch (Exception ignored) {
            // 预期内异常，仅验证 MDC 清理
        }

        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }
}