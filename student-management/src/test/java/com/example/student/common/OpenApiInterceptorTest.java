package com.example.student.common;

import com.example.student.mapper.ApiIdempotencyMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 开放接口适配层：限流、幂等键缺失拒绝、重复键回放。 */
class OpenApiInterceptorTest {

    private ApiIdempotencyMapper mapper;
    private OpenApiInterceptor interceptor;

    @BeforeEach
    void setUp() throws Exception {
        mapper = mock(ApiIdempotencyMapper.class);
        interceptor = new OpenApiInterceptor();
        ReflectionTestUtils.setField(interceptor, "idempotencyMapper", mapper);
        when(mapper.insert(any())).thenReturn(1);
        when(mapper.insertAudit(any())).thenReturn(1);
    }

    private HttpServletRequest req(String method, String path, String idemKey) {
        HttpServletRequest r = mock(HttpServletRequest.class);
        when(r.getMethod()).thenReturn(method);
        when(r.getRequestURI()).thenReturn(path);
        when(r.getHeader("X-Client-Id")).thenReturn("partner-a");
        when(r.getHeader("Idempotency-Key")).thenReturn(idemKey);
        return r;
    }

    private HttpServletResponse resp() throws Exception {
        HttpServletResponse r = mock(HttpServletResponse.class);
        StringWriter sw = new StringWriter();
        when(r.getWriter()).thenReturn(new PrintWriter(sw, true));
        return r;
    }

    @Test
    void writeWithoutIdempotencyKeyRejected400() throws Exception {
        HttpServletRequest req = req("POST", "/api/v1/students", null);
        HttpServletResponse resp = resp();

        boolean ok = interceptor.preHandle(req, resp, new Object());

        assertFalse(ok);
        verify(resp).setStatus(400);
    }

    @Test
    void duplicateKeyReplaysStoredResponse() throws Exception {
        ApiIdempotency stored = new ApiIdempotency();
        stored.setResponseBody("{\"code\":200}");
        stored.setHttpStatus(200);
        when(mapper.selectOne(any())).thenReturn(stored);

        HttpServletRequest req = req("POST", "/api/v1/students", "key-1");
        HttpServletResponse resp = resp();

        boolean ok = interceptor.preHandle(req, resp, new Object());

        assertFalse(ok, "replayed request must not reach controller");
        verify(resp).setStatus(200);
        verify(resp).setHeader("Idempotent-Replayed", "true");
    }

    @Test
    void firstTimeKeyInsertsPlaceholderAndPasses() throws Exception {
        when(mapper.selectOne(any())).thenReturn(null);

        HttpServletRequest req = req("POST", "/api/v1/students", "key-2");
        HttpServletResponse resp = resp();

        assertTrue(interceptor.preHandle(req, resp, new Object()));
        ArgumentCaptor<ApiIdempotency> cap = ArgumentCaptor.forClass(ApiIdempotency.class);
        verify(mapper).insert(cap.capture());
        assertEquals("key-2", cap.getValue().getIdempotencyKey());
    }

    @Test
    void rateLimitBlocksAfterThreshold() throws Exception {
        String path = "/api/v1/ping";
        int allowed = 0, blocked = 0;
        for (int i = 0; i < 125; i++) {
            HttpServletRequest req = req("GET", path, null);
            HttpServletResponse resp = resp();
            // GET 无幂等要求，直接走限流
            if (interceptor.preHandle(req, resp, new Object())) allowed++;
            else blocked++;
        }
        assertEquals(120, allowed);
        assertEquals(5, blocked);
    }
}
