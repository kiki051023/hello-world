package com.example.student.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase8PasswordPolicyMigrationTest {

    private static final Path MIGRATION = Path.of("src/main/resources/sql/migrations/phase8_password_policy.sql");

    @Test
    void migrationAddsPasswordPolicyColumnsIdempotently() throws Exception {
        String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase();

        // 幂等：列存在性判断 + 可重复执行（procedure guard）
        assertTrue(sql.contains("information_schema.columns"));
        assertTrue(sql.contains("must_change_password"));
        assertTrue(sql.contains("last_password_change"));
        assertTrue(sql.contains("drop procedure if exists phase8_add_column"));
        assertFalse(sql.contains("drop table"), "migration must never drop tables");
        assertFalse(sql.contains("truncate"), "migration must never truncate");
    }
}