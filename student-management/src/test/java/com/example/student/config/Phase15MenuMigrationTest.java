package com.example.student.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** M4-6 动态菜单迁移结构测试 */
class Phase15MenuMigrationTest {

    private static final Path MIGRATION = Path.of("src/main/resources/sql/migrations/phase15_menu.sql");

    @Test
    void migrationCreatesMenuTableWithSeeds() throws Exception {
        String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase();

        assertTrue(sql.contains("create table if not exists sys_menu"), "幂等建表");
        assertTrue(sql.contains("uk_menu_path"), "path 唯一键");
        assertTrue(sql.contains("roles"), "角色列");
        // 种子覆盖现有 21 个菜单路径
        for (String p : new String[]{"/dashboard", "/analysis", "/portal", "/score", "/system/terms",
                "/score-audit", "/education", "/timetable", "/student", "/clazz", "/course", "/teacher",
                "/affairs", "/practice", "/ops", "/system/org", "/system/users", "/system/roles",
                "/system/dict", "/system/dorm"}) {
            assertTrue(sql.contains("'" + p + "'"), "种子路径: " + p);
        }
        assertTrue(sql.contains("where not exists"), "种子幂等保护");

        assertFalse(sql.contains("drop table"), "migration must never drop tables");
        assertFalse(sql.contains("truncate"), "migration must never truncate");
        assertFalse(sql.contains("delete from"), "migration must never delete rows");
        assertFalse(sql.contains("use "), "migration must not switch database");
    }
}
