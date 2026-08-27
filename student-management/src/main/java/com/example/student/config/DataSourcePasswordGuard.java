package com.example.student.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 阶段七安全加固：数据库密码不再提供硬编码回退值。
 *
 * application.yml 中已移除 ${DB_PASSWORD:123456} 默认值，此守卫确保：
 * 1. 未通过环境变量 DB_PASSWORD / JVM 参数 -Dspring.datasource.password 注入密码时，启动即失败——
 *    杜绝"忘记配置却静默连上默认密码库"的风险。
 * 2. 本地开发密码（如 123456）必须由启动脚本（start-all.bat）或 IDE 显式注入，
 *    生产部署则必须配置强密码；守卫不校验具体取值，只确保"显式提供"。
 */
@Configuration
public class DataSourcePasswordGuard {

    @Value("${spring.datasource.password}")
    private String password;

    @PostConstruct
    void guard() {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                "缺少 DB_PASSWORD 环境变量：拒绝以空数据库密码启动。请设置环境变量 DB_PASSWORD 或 JVM 参数 -Dspring.datasource.password=<数据库密码>");
        }
    }
}