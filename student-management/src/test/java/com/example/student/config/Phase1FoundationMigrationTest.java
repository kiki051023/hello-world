package com.example.student.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase1FoundationMigrationTest {

    private static final Path MIGRATION = Path.of("src/main/resources/sql/migrations/phase1_foundation.sql");

    @Test
    void migrationContainsIdempotentFoundationSchemaAndCompatibilityColumns() throws Exception {
        String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase();

        for (String table : new String[]{"school", "department", "major", "academic_term", "sys_role",
                "sys_permission", "sys_user_role", "sys_role_permission", "sys_data_scope"}) {
            assertTrue(sql.contains("create table if not exists " + table), "missing idempotent table: " + table);
        }
        for (String column : new String[]{"school_id", "department_id", "major_id", "student_status",
                "education_level", "political_status", "graduation_year", "profile_version", "employee_type",
                "teacher_type", "employment_status", "permission_version"}) {
            assertTrue(sql.contains("'" + column + "'"), "missing compatibility column: " + column);
        }
        assertTrue(sql.contains("drop procedure if exists phase1_add_column"));
        assertTrue(sql.contains("insert ignore into sys_user_role"));
        assertTrue(sql.contains("on duplicate key update"));
        for (String constraint : new String[]{"uk_school_code", "uk_department_school_code", "uk_major_department_code",
                "uk_academic_term", "uk_sys_role_code", "uk_sys_permission_code", "primary key (user_id, role_id)",
                "primary key (role_id, permission_id)", "idx_data_scope_value (scope_type, scope_value)"}) {
            assertTrue(sql.contains(constraint), "missing key constraint/index: " + constraint);
        }
        assertFalse(sql.contains("drop table"), "migration must never drop business tables");
    }
}
