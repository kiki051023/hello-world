package com.example.student.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** M2-3 宿舍示例表迁移结构测试 */
class Phase12DormMigrationTest {

    private static final Path MIGRATION = Path.of("src/main/resources/sql/migrations/phase12_dorm.sql");

    @Test
    void migrationCreatesDormTableIdempotently() throws Exception {
        String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase();

        assertTrue(sql.contains("create table if not exists dorm_room"), "幂等建表");
        assertTrue(sql.contains("building"), "楼栋");
        assertTrue(sql.contains("room_no"), "房间号");
        assertTrue(sql.contains("capacity"), "容量");
        assertTrue(sql.contains("uk_dorm_room"), "楼栋+房间号唯一");

        assertFalse(sql.contains("drop table"), "migration must never drop tables");
        assertFalse(sql.contains("truncate"), "migration must never truncate");
        assertFalse(sql.contains("delete from"), "migration must never delete rows");
        assertFalse(sql.contains("use "), "migration must not switch database");
    }
}
