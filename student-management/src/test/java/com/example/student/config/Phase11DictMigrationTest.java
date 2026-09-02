package com.example.student.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** M2-1 数据字典迁移结构测试：幂等建表 + 唯一约束 + 首批种子 + 安全红线 */
class Phase11DictMigrationTest {

    private static final Path MIGRATION = Path.of("src/main/resources/sql/migrations/phase11_dict.sql");

    @Test
    void migrationCreatesDictTablesIdempotently() throws Exception {
        String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase();

        // 幂等建表
        assertTrue(sql.contains("create table if not exists sys_dict_type"), "字典类型表幂等");
        assertTrue(sql.contains("create table if not exists sys_dict_data"), "字典数据表幂等");

        // 核心字段
        assertTrue(sql.contains("dict_type"), "类型标识");
        assertTrue(sql.contains("dict_value"), "字典值");
        assertTrue(sql.contains("dict_label"), "字典标签");
        assertTrue(sql.contains("sort"), "排序");

        // 唯一约束
        assertTrue(sql.contains("uk_dict_type"), "类型唯一键");
        assertTrue(sql.contains("uk_dict_type_value"), "类型+值唯一键");

        // 首批 9 类种子
        for (String t : new String[]{"gender", "education", "reward_type", "leave_type",
                "funding_type", "warning_level", "employment_status", "score_status", "practice_log_status"}) {
            assertTrue(sql.contains("'" + t + "'"), "种子类型: " + t);
        }

        // 幂等种子（WHERE NOT EXISTS）
        assertTrue(sql.contains("where not exists"), "种子幂等保护");

        // 安全红线
        assertFalse(sql.contains("drop table"), "migration must never drop tables");
        assertFalse(sql.contains("truncate"), "migration must never truncate");
        assertFalse(sql.contains("delete from"), "migration must never delete rows");
        assertFalse(sql.contains("use "), "migration must not switch database");
        assertFalse(sql.contains("create database"), "migration must not create database");
    }
}
