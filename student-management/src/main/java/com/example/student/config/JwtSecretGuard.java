package com.example.student.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 阶段六安全加固：JWT 密钥不再提供硬编码回退值。
 *
 * 生产/默认 profile 下必须通过环境变量 JWT_SECRET 注入真实密钥，
 * 否则应用启动即失败——杜绝使用公开已知密钥签发可伪造令牌的风险。
 * 本地开发可用 -Djwt.secret=... 或 IDE 环境变量注入。
 */
@Configuration
public class JwtSecretGuard {

    @Value("${jwt.secret}")
    private String secret;

    @PostConstruct
    void guard() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "缺少 JWT_SECRET 环境变量：拒绝以空密钥启动。请设置环境变量或 JVM 参数 -Djwt.secret=<强随机密钥>");
        }
        if (secret.startsWith("student-management-system-secret-key")) {
            throw new IllegalStateException(
                "检测到已泄露的示例 JWT_SECRET：为防止令牌伪造，拒绝启动。请在环境变量 JWT_SECRET 中配置强随机密钥");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET 强度不足（<32 字符），HS256 要求至少 256 位密钥");
        }
    }
}
