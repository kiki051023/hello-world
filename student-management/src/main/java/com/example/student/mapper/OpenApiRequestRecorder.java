package com.example.student.mapper;

import com.example.student.common.OpenApiInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 桥接：controller 通过它调用拦截器的响应登记方法，避免直接依赖拦截器类。 */
@Component
public class OpenApiRequestRecorder {

    private final OpenApiInterceptor interceptor;

    public OpenApiRequestRecorder(@Autowired(required = false) OpenApiInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    public void record(HttpServletRequest request, int status, String bodyJson) {
        if (interceptor != null && request.getRequestURI().startsWith("/api/v1/")) {
            interceptor.recordResponse(request, status, bodyJson);
        }
    }
}
