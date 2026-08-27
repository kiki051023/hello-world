package com.example.student.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthCheckServiceTest {

    private DataSource dataSourceUp() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(connection.isValid(anyInt())).thenReturn(true);
        when(dataSource.getConnection()).thenReturn(connection);
        return dataSource;
    }

    private StringRedisTemplate redisUp() {
        RedisConnection redisConnection = mock(RedisConnection.class);
        when(redisConnection.ping()).thenReturn("PONG");
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        when(factory.getConnection()).thenReturn(redisConnection);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.getConnectionFactory()).thenReturn(factory);
        return redisTemplate;
    }

    @Test
    void allUpWhenDbAndRedisRespond() throws Exception {
        HealthCheckService service = new HealthCheckService(dataSourceUp(), redisUp());

        Map<String, String> checks = service.checkDependencies();

        assertThat(checks).containsEntry("db", "UP").containsEntry("redis", "UP");
        assertThat(service.overallStatus(checks)).isEqualTo("UP");
    }

    @Test
    void detailedContainsStatusChecksAndTimestamp() throws Exception {
        HealthCheckService service = new HealthCheckService(dataSourceUp(), redisUp());

        Map<String, Object> detailed = service.detailed();

        assertThat(detailed.get("status")).isEqualTo("UP");
        assertThat(detailed).containsKeys("checks", "timestamp");
        assertThat(((Map<?, ?>) detailed.get("checks")).get("db")).isEqualTo("UP");
    }

    @Test
    void degradedWhenDatabaseDown() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Can not connect to MySQL"));

        HealthCheckService service = new HealthCheckService(dataSource, redisUp());

        Map<String, String> checks = service.checkDependencies();
        assertThat(checks.get("db")).startsWith("DOWN");
        assertThat(service.overallStatus(checks)).isEqualTo("DEGRADED");
    }

    @Test
    void degradedWhenRedisDown() throws Exception {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        when(factory.getConnection()).thenThrow(new RuntimeException("Connection refused: localhost/127.0.0.1:6379"));
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.getConnectionFactory()).thenReturn(factory);

        HealthCheckService service = new HealthCheckService(dataSourceUp(), redisTemplate);

        Map<String, String> checks = service.checkDependencies();
        assertThat(checks.get("redis")).startsWith("DOWN");
        assertThat(service.overallStatus(checks)).isEqualTo("DEGRADED");
    }
}