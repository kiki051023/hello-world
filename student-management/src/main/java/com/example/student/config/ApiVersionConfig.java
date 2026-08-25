package com.example.student.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 阶段六开放接口适配层之一：API 版本化（简化安全方案）。
 *
 * Spring Boot 3.2 的 PathPatternParser 不支持同一 controller 双前缀注册，
 * 因此采用「显式版本路由 + 兼容映射」策略：
 *   - 新外部系统一律使用 /api/v1/**（由 ApiV1Controller 显式声明）
 *   - 旧前端继续用 /api/**，行为不变
 */
@Configuration
public class ApiVersionConfig {

    /** 版本标识，供响应头与文档引用 */
    public static final String API_VERSION = "v1";
}
