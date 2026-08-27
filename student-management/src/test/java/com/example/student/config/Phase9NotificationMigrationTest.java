package com.example.student.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** P2-D 通知中心迁移结构测试：幂等（IF NOT EXISTS）+ 关键字段 + 不删表 */
class Phase9NotificationMigrationTest {

    private static final Path MIGRATION = Path.of("src/main/resources/sql/migrations/phase9_notification.sql");

    @Test
    void migrationCreatesNotificationTableIdempotently() throws Exception {
        String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase();

        // 幂等建表 + 核心字段
        assertTrue(sql.contains("create table if not exists sys_notification"), "幂等建表");
        assertTrue(sql.contains("receiver_id"), "接收人字段");
        assertTrue(sql.contains("title"), "标题");
        assertTrue(sql.contains("content"), "内容");
        assertTrue(sql.contains("type"), "类型");
        assertTrue(sql.contains("related_id"), "关联业务ID");
        assertTrue(sql.contains("is_read"), "已读标记");
        assertTrue(sql.contains("idx_notification_receiver"), "接收人索引");
        assertTrue(sql.contains("idx_notification_receiver_read"), "接收人+已读复合索引");

        // 安全红线
        assertFalse(sql.contains("drop table"), "migration must never drop tables");
        assertFalse(sql.contains("truncate"), "migration must never truncate");
        assertFalse(sql.contains("delete from"), "migration must never delete rows");
        assertFalse(sql.contains("use "), "migration must not switch database");
        assertFalse(sql.contains("create database"), "migration must not create database");
    }
}