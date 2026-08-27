package com.example.student.service;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查：逐项真实探测依赖（数据库 / Redis）。
 * <ul>
 *   <li>{@link #checkDependencies()}：每个依赖的探测结果（UP / DOWN:原因）</li>
 *   <li>{@link #overallStatus(Map)}：任一依赖 DOWN =&gt; DEGRADED，全部 UP =&gt; UP</li>
 *   <li>{@link #detailed()}：汇总 + 明细 + 时间戳（{@code /api/system/health/detailed} 数据源）</li>
 * </ul>
 */
@Service
public class HealthCheckService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;

    public HealthCheckService(DataSource dataSource, StringRedisTemplate redisTemplate) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
    }

    public Map<String, String> checkDependencies() {
        Map<String, String> checks = new LinkedHashMap<>();
        checks.put("db", checkDb());
        checks.put("redis", checkRedis());
        return checks;
    }

    public String overallStatus(Map<String, String> checks) {
        return checks.values().stream().anyMatch(v -> v.startsWith("DOWN")) ? "DEGRADED" : "UP";
    }

    public Map<String, Object> detailed() {
        Map<String, String> checks = checkDependencies();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", overallStatus(checks));
        data.put("checks", checks);
        data.put("timestamp", LocalDateTime.now().format(TIME_FORMAT));
        return data;
    }

    /** 数据库探针：真实获取连接池连接并执行 isValid（MySQL 驱动底层为 SELECT 1）。 */
    private String checkDb() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN: " + rootMessage(e);
        }
    }

    /** Redis 探针：真实建立连接并执行 PING。 */
    private String checkRedis() {
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        if (factory == null) {
            return "DOWN: no connection factory";
        }
        RedisConnection connection = null;
        try {
            connection = factory.getConnection();
            String pong = connection.ping();
            return "PONG".equalsIgnoreCase(pong) ? "UP" : "DOWN: unexpected response " + pong;
        } catch (Exception e) {
            return "DOWN: " + rootMessage(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ignored) {
                    // 关闭失败不影响探测结论
                }
            }
        }
    }

    /** 取异常链最深层的可读信息，避免暴露 Lettuce 内部包装噪声。 */
    private String rootMessage(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }
}