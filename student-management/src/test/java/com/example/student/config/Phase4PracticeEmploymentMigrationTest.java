package com.example.student.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase4PracticeEmploymentMigrationTest {

    private static final Path MIGRATION = Path.of("src/main/resources/sql/migrations/phase4_practice_employment.sql");

    @Test
    void migrationContainsIdempotentPracticeAndEmploymentSchema() throws Exception {
        String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase();

        for (String table : new String[]{"practice_company", "practice_position", "practice_plan",
                "practice_assignment", "practice_log", "practice_evaluation",
                "attachment_object", "employment_record", "student_certificate"}) {
            assertTrue(sql.contains("create table if not exists " + table), "missing table: " + table);
        }
        for (String key : new String[]{
                "uk_practice_plan (academic_year, plan_name)",
                "uk_practice_assign (plan_id, student_id)",
                "uk_plog_week (assignment_id, week_no)",
                "uk_peval_once (assignment_id, evaluator_type)",
                "uk_attachment_key (object_key)",
                "uk_employ_year (student_id, graduation_year)"}) {
            assertTrue(sql.contains(key), "missing unique key: " + key);
        }
        assertFalse(sql.contains("drop table"), "migration must never drop business tables");
    }

    @Test
    void attachmentsStoreObjectKeysNotPhysicalPaths() throws Exception {
        String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase();
        assertTrue(sql.contains("object_key"), "attachment must store object key");
        assertFalse(sql.contains("file_path") || sql.contains("physical_path"),
                "attachment must not store physical path");
    }
}
