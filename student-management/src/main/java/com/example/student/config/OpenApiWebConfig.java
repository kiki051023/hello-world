package com.example.student.config;

import com.example.student.common.OpenApiInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 注册开放接口适配层拦截器：仅作用于 /api/v1/**。 */
@Configuration
public class OpenApiWebConfig implements WebMvcConfigurer {

    @Autowired
    private OpenApiInterceptor openApiInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(openApiInterceptor).addPathPatterns("/api/v1/**");
    }
}
