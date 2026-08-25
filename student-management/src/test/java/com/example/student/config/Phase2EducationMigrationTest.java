package com.example.student.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase2EducationMigrationTest {

    private static final Path MIGRATION = Path.of("src/main/resources/sql/migrations/phase2_education_scores.sql");

    @Test
    void migrationContainsIdempotentEducationSchemaAndScoreLifecycleColumns() throws Exception {
        String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase();

        for (String table : new String[]{"course_term", "teaching_task", "training_plan",
                "training_plan_course", "course_selection", "score_change_log"}) {
            assertTrue(sql.contains("create table if not exists " + table), "missing idempotent table: " + table);
        }
        for (String column : new String[]{"course_term_id", "score_status", "score_type",
                "active", "original_score_id", "version_no", "active_uk"}) {
            assertTrue(sql.contains("'" + column + "'"), "missing compatibility column: " + column);
        }
        for (String key : new String[]{"uk_course_term (term_id, course_id)",
                "uk_teaching_task (course_term_id, teacher_id, clazz_id)",
                "uk_training_plan (major_id, enrollment_year)",
                "uk_plan_course (plan_id, course_id)",
                "uk_course_selection (course_term_id, student_id)",
                "uk_score_active (active_uk)"}) {
            assertTrue(sql.contains(key), "missing key constraint: " + key);
        }
        assertTrue(sql.contains("drop index uk_student_course"), "legacy score unique key must be replaced");
        assertTrue(sql.contains("v_phase2_score_conflict"), "pre-migration conflict report view missing");
        assertFalse(sql.contains("drop table"), "migration must never drop business tables");
    }

    @Test
    void migrationKeepsLifecycleDefaultsCompatibleWithLegacyRows() throws Exception {
        String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase();
        assertTrue(sql.contains("default ''approved''"), "legacy rows must stay APPROVED/active");
        assertTrue(sql.contains("default ''regular''"), "legacy rows must keep REGULAR type");
        assertTrue(sql.contains("generated always"), "active uniqueness must use generated column");
    }
}
