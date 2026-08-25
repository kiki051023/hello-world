-- 阶段二：教务与成绩迁移（MySQL 8.0+）
--
-- 用法：在已完成阶段一迁移的 student_db 上直接执行本脚本。
-- 本脚本不由应用自动执行；重复执行不会删除业务数据，也不会重复创建列、表或索引。
-- 成绩生命周期：DRAFT/SUBMITTED/APPROVED/REJECTED/VOID；类型：REGULAR/MAKEUP/RETAKE。
-- 只有 APPROVED 记录占用 active=1；同一(学生,课程,类型,学期)最多一条有效记录。

USE student_db;

CREATE TABLE IF NOT EXISTS course_term (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    term_id BIGINT NOT NULL COMMENT '学年学期ID(academic_term.id)',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    capacity INT NOT NULL DEFAULT 0 COMMENT '容量',
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_course_term (term_id, course_id),
    KEY idx_course_term_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程开设（开课学期）';

CREATE TABLE IF NOT EXISTS teaching_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_term_id BIGINT NOT NULL COMMENT '开课ID(course_term.id)',
    teacher_id BIGINT NOT NULL COMMENT '授课教师ID',
    clazz_id BIGINT NULL COMMENT '授课班级ID',
    task_role VARCHAR(20) NOT NULL DEFAULT 'LECTURER' COMMENT 'LECTURER主讲/ASSISTANT助教',
    weekly_hours DECIMAL(3,1) NULL COMMENT '周学时',
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_teaching_task (course_term_id, teacher_id, clazz_id),
    KEY idx_teaching_task_teacher (teacher_id),
    KEY idx_teaching_task_clazz (clazz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师授课任务';

CREATE TABLE IF NOT EXISTS training_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    major_id BIGINT NOT NULL COMMENT '专业ID',
    enrollment_year SMALLINT NOT NULL COMMENT '适用入学年份',
    plan_name VARCHAR(100) NOT NULL,
    total_credits DECIMAL(5,1) NULL COMMENT '总学分要求',
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_training_plan (major_id, enrollment_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培养方案';

CREATE TABLE IF NOT EXISTS training_plan_course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL COMMENT '培养方案ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    course_type VARCHAR(20) NOT NULL DEFAULT 'REQUIRED' COMMENT 'REQUIRED必修/ELECTIVE选修',
    suggest_term_no TINYINT NULL COMMENT '建议修读学期',
    credit DECIMAL(3,1) NULL COMMENT '学分',
    UNIQUE KEY uk_plan_course (plan_id, course_id),
    KEY idx_plan_course_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培养方案课程';

CREATE TABLE IF NOT EXISTS course_selection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_term_id BIGINT NOT NULL COMMENT '开课ID(course_term.id)',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    selection_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL正常/MAKEUP补选',
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_course_selection (course_term_id, student_id),
    KEY idx_course_selection_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生选课';

-- 成绩生命周期扩展列（兼容旧库：存在即跳过）。
DROP PROCEDURE IF EXISTS phase2_add_column;
DELIMITER $$
CREATE PROCEDURE phase2_add_column(IN p_table VARCHAR(64), IN p_column VARCHAR(64),
                                   IN p_definition VARCHAR(500) CHARACTER SET utf8mb4)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column
    ) THEN
        SET @phase2_ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE phase2_stmt FROM @phase2_ddl;
        EXECUTE phase2_stmt;
        DEALLOCATE PREPARE phase2_stmt;
    END IF;
END$$
DELIMITER ;

CALL phase2_add_column('score', 'course_term_id', 'BIGINT NOT NULL DEFAULT 0 COMMENT ''开课ID,0为历史数据未关联''');
CALL phase2_add_column('score', 'score_status', 'VARCHAR(20) NOT NULL DEFAULT ''APPROVED'' COMMENT ''DRAFT/SUBMITTED/APPROVED/REJECTED/VOID''');
CALL phase2_add_column('score', 'score_type', 'VARCHAR(20) NOT NULL DEFAULT ''REGULAR'' COMMENT ''REGULAR/MAKEUP/RETAKE''');
CALL phase2_add_column('score', 'active', 'TINYINT NOT NULL DEFAULT 1 COMMENT ''当前有效版本标志''');
CALL phase2_add_column('score', 'original_score_id', 'BIGINT NULL COMMENT ''补考/重修关联的原成绩ID''');
CALL phase2_add_column('score', 'version_no', 'INT NOT NULL DEFAULT 1 COMMENT ''业务版本号''');
CALL phase2_add_column('score', 'version', 'INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本''');
DROP PROCEDURE IF EXISTS phase2_add_column;

-- 有效版本唯一约束：仅 active=1 的记录参与唯一性（生成列为 NULL 时不冲突），
-- 替换旧 uk_student_course(student_id, course_id)，否则无法写入补考/重修记录。
SET @phase2_has_old_uk := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'score' AND index_name = 'uk_student_course');
SET @phase2_ddl := IF(@phase2_has_old_uk > 0,
    'ALTER TABLE score DROP INDEX uk_student_course', 'SELECT 1');
PREPARE phase2_stmt FROM @phase2_ddl;
EXECUTE phase2_stmt;
DEALLOCATE PREPARE phase2_stmt;

SET @phase2_has_gen := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'score' AND column_name = 'active_uk');
SET @phase2_ddl := IF(@phase2_has_gen = 0,
    'ALTER TABLE score ADD COLUMN active_uk VARCHAR(150) GENERATED ALWAYS AS (CASE WHEN active = 1 THEN CONCAT_WS('':'', student_id, course_id, score_type, course_term_id) ELSE NULL END) STORED, ADD UNIQUE KEY uk_score_active (active_uk)',
    'SELECT 1');
PREPARE phase2_stmt FROM @phase2_ddl;
EXECUTE phase2_stmt;
DEALLOCATE PREPARE phase2_stmt;

-- 迁移前冲突报告辅助视图：同一学生+课程的旧成绩本就受唯一键约束，理论上应为空；
-- 若历史库曾绕过约束产生重复，此处列出以便人工处理后再启用唯一键。
DROP VIEW IF EXISTS v_phase2_score_conflict;
CREATE VIEW v_phase2_score_conflict AS
SELECT student_id, course_id, COUNT(*) AS dup_count
FROM score WHERE deleted = 0
GROUP BY student_id, course_id HAVING COUNT(*) > 1;

CREATE TABLE IF NOT EXISTS score_change_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    score_id BIGINT NOT NULL COMMENT '成绩ID',
    change_type VARCHAR(30) NOT NULL COMMENT 'CREATE/SUBMIT/APPROVE/REJECT/VOID/CHANGE/MAKEUP/RETAKE',
    old_score DECIMAL(5,2) NULL,
    new_score DECIMAL(5,2) NULL,
    reason VARCHAR(255) NULL,
    operator_id BIGINT NULL,
    operator_name VARCHAR(100) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_scl_score (score_id),
    KEY idx_scl_created (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩变更历史（只追加）';
