package com.example.student.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** M1-1 定时任务框架迁移结构测试：幂等建表 + seed + 关键字段 + 不删表/切库 */
class Phase10ScheduledTasksMigrationTest {

    private static final Path MIGRATION = Path.of("src/main/resources/sql/migrations/phase10_scheduled_tasks.sql");

    @Test
    void migrationCreatesScheduledTablesIdempotently() throws Exception {
        String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase();

        // 幂等建表 + 表结构
        assertTrue(sql.contains("create table if not exists sys_scheduled_task"), "幂等建任务配置表");
        assertTrue(sql.contains("create table if not exists sys_scheduled_task_log"), "幂等建任务日志表");
        assertTrue(sql.contains("task_code"), "任务编码");
        assertTrue(sql.contains("cron"), "cron 表达式");
        assertTrue(sql.contains("enabled"), "启用开关");
        assertTrue(sql.contains("status"), "执行状态");
        assertTrue(sql.contains("cost_ms"), "耗时字段");
        assertTrue(sql.contains("uk_scheduled_task_code"), "任务编码唯一键");

        // 内置 seed 幂等（INSERT IGNORE）
        assertTrue(sql.contains("insert ignore into sys_scheduled_task"), "seed 幂等写入");
        assertTrue(sql.contains("warning_scan"), "预警扫描任务");
        assertTrue(sql.contains("term_auto_switch"), "学期切换任务");
        assertTrue(sql.contains("notify_cleanup"), "通知清理任务");
        assertTrue(sql.contains("import_reaper"), "导入回收任务");

        // 安全红线
        assertFalse(sql.contains("drop table"), "migration must never drop tables");
        assertFalse(sql.contains("truncate"), "migration must never truncate");
        assertFalse(sql.contains("delete from"), "migration must never delete rows");
        assertFalse(sql.contains("use "), "migration must not switch database");
        assertFalse(sql.contains("create database"), "migration must not create database");
    }
}