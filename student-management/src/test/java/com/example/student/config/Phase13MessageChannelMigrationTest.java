package com.example.student.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** M3-1 消息多渠道迁移结构测试：email 列守护 + 渠道日志表 + 安全红线 */
class Phase13MessageChannelMigrationTest {

    private static final Path MIGRATION = Path.of("src/main/resources/sql/migrations/phase13_message_channel.sql");

    @Test
    void migrationAddsEmailAndChannelLog() throws Exception {
        String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase();

        // email 列（信息_schema 守护幂等）
        assertTrue(sql.contains("information_schema.columns"), "ALTER 幂等守护");
        assertTrue(sql.contains("add column email"), "sys_user.email");

        // 渠道日志表
        assertTrue(sql.contains("create table if not exists sys_notify_channel_log"), "渠道日志幂等建表");
        assertTrue(sql.contains("notification_id"), "关联通知");
        assertTrue(sql.contains("channel"), "渠道");
        assertTrue(sql.contains("retry_count"), "重试计数");
        assertTrue(sql.contains("idx_channel_log_status"), "重试扫描索引");

        // 安全红线
        assertFalse(sql.contains("drop table"), "migration must never drop tables");
        assertFalse(sql.contains("truncate"), "migration must never truncate");
        assertFalse(sql.contains("delete from"), "migration must never delete rows");
        assertFalse(sql.contains("use "), "migration must not switch database");
    }
}
