package com.example.student.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** M4-2 课表时间槽迁移结构测试 */
class Phase14TimetableMigrationTest {

    private static final Path MIGRATION = Path.of("src/main/resources/sql/migrations/phase14_timetable.sql");

    @Test
    void migrationAddsTimeSlotColumnsIdempotently() throws Exception {
        String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase();

        assertTrue(sql.contains("information_schema.columns"), "ALTER 幂等守护");
        assertTrue(sql.contains("add column week_day"), "星期字段");
        assertTrue(sql.contains("add column period"), "节次字段");
        assertTrue(sql.contains("add column classroom"), "教室字段");

        assertFalse(sql.contains("drop table"), "migration must never drop tables");
        assertFalse(sql.contains("truncate"), "migration must never truncate");
        assertFalse(sql.contains("delete from"), "migration must never delete rows");
        assertFalse(sql.contains("use "), "migration must not switch database");
    }
}
