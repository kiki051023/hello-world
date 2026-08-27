-- ============================================================
-- 阶段八（P1）：密码策略与账号安全
--   1. sys_user.must_change_password：首次登录/管理员重置密码后强制改密
--   2. sys_user.last_password_change：最近改密时间（预留密码过期审计）
--   3. 幂等：基于 information_schema 列存在性判断
-- ============================================================

CREATE DATABASE IF NOT EXISTS student_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE student_db;

-- 幂等添加列（先判断存在性再 ALTER）
DROP PROCEDURE IF EXISTS phase8_add_column;
DELIMITER $$
CREATE PROCEDURE phase8_add_column(IN p_table VARCHAR(64), IN p_column VARCHAR(64),
                                   IN p_definition VARCHAR(255) CHARACTER SET utf8mb4)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column
    ) THEN
        SET @phase8_ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE phase8_stmt FROM @phase8_ddl;
        EXECUTE phase8_stmt;
        DEALLOCATE PREPARE phase8_stmt;
    END IF;
END$$
DELIMITER ;

CALL phase8_add_column('sys_user', 'must_change_password', 'TINYINT NOT NULL DEFAULT 0 COMMENT ''首次登录/重置后需强制改密''');
CALL phase8_add_column('sys_user', 'last_password_change', 'DATETIME NULL COMMENT ''最近改密时间''');
DROP PROCEDURE IF EXISTS phase8_add_column;