package com.example.student.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase3StudentAffairsMigrationTest {

    private static final Path MIGRATION = Path.of("src/main/resources/sql/migrations/phase3_student_affairs.sql");

    @Test
    void migrationContainsIdempotentAffairsSchema() throws Exception {
        String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase();

        for (String table : new String[]{"student_status_history", "leave_request",
                "reward_punishment", "hardship_funding", "student_warning"}) {
            assertTrue(sql.contains("create table if not exists " + table), "missing table: " + table);
        }
        for (String key : new String[]{
                "uk_funding_once (student_id, academic_year, funding_type)",
                "uk_warning_active (student_id, warning_type, rule_key, status)"}) {
            assertTrue(sql.contains(key), "missing unique key: " + key);
        }
        assertFalse(sql.contains("drop table"), "migration must never drop business tables");
    }

    @Test
    void sensitiveDataStaysInsideFundingTable() throws Exception {
        String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase();
        assertTrue(sql.contains("sensitive_note"), "sensitive note must live in the funding table only");
        // 请假/预警等非敏感表不得出现敏感字段
        String leaveSection = sql.substring(sql.indexOf("leave_request"));
        assertFalse(leaveSection.startsWith("sensitive"), "no sensitive fields in non-sensitive tables");
    }
}
