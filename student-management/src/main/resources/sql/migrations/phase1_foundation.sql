-- 阶段一基础平台迁移（MySQL 8.0+）
--
-- 用法：先执行 sql/init.sql（新库）或在现有 student_db 上直接执行本脚本。
-- 本脚本不由应用自动执行；重复执行不会删除业务数据，也不会重复创建列、表或种子数据。

CREATE DATABASE IF NOT EXISTS student_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE student_db;

CREATE TABLE IF NOT EXISTS school (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_code VARCHAR(50) NOT NULL,
    school_name VARCHAR(100) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_school_code (school_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学校/校区';

CREATE TABLE IF NOT EXISTS department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    department_code VARCHAR(50) NOT NULL,
    department_name VARCHAR(100) NOT NULL,
    leader_user_id BIGINT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_department_school_code (school_id, department_code),
    KEY idx_department_school (school_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系部/二级学院';

CREATE TABLE IF NOT EXISTS major (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_id BIGINT NOT NULL,
    major_code VARCHAR(50) NOT NULL,
    major_name VARCHAR(100) NOT NULL,
    education_level VARCHAR(30),
    duration_years DECIMAL(3,1),
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_major_department_code (department_id, major_code),
    KEY idx_major_department (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专业';

CREATE TABLE IF NOT EXISTS academic_term (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year VARCHAR(20) NOT NULL,
    term_no TINYINT NOT NULL,
    term_name VARCHAR(50) NOT NULL,
    start_date DATE,
    end_date DATE,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0非当前 1当前',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_academic_term (academic_year, term_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学年学期';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    resource_type VARCHAR(20) NOT NULL DEFAULT 'API',
    resource_path VARCHAR(255),
    parent_id BIGINT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_permission_code (permission_code),
    KEY idx_permission_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限';

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id),
    KEY idx_role_permission_permission (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联';

CREATE TABLE IF NOT EXISTS sys_data_scope (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    role_id BIGINT NULL,
    scope_type VARCHAR(30) NOT NULL COMMENT 'SCHOOL/DEPARTMENT/MAJOR/CLAZZ/COURSE/SELF',
    scope_value BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_data_scope_user (user_id),
    KEY idx_data_scope_role (role_id),
    KEY idx_data_scope_value (scope_type, scope_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据权限范围';

-- 兼容旧库：通过信息_schema 判断列是否存在，避免重复 ALTER 失败。
DROP PROCEDURE IF EXISTS phase1_add_column;
DELIMITER $$
CREATE PROCEDURE phase1_add_column(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition VARCHAR(255))
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column
    ) THEN
        SET @phase1_ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE phase1_stmt FROM @phase1_ddl;
        EXECUTE phase1_stmt;
        DEALLOCATE PREPARE phase1_stmt;
    END IF;
END$$
DELIMITER ;

CALL phase1_add_column('sys_user', 'school_id', 'BIGINT NULL COMMENT ''所属学校''');
CALL phase1_add_column('sys_user', 'department_id', 'BIGINT NULL COMMENT ''所属系部''');
CALL phase1_add_column('sys_user', 'employee_type', 'VARCHAR(30) NULL COMMENT ''人员类型''');
CALL phase1_add_column('clazz', 'school_id', 'BIGINT NULL COMMENT ''所属学校''');
CALL phase1_add_column('clazz', 'department_id', 'BIGINT NULL COMMENT ''所属系部''');
CALL phase1_add_column('clazz', 'major_id', 'BIGINT NULL COMMENT ''所属专业''');
CALL phase1_add_column('clazz', 'admission_year', 'SMALLINT NULL COMMENT ''入学年份''');
CALL phase1_add_column('clazz', 'academic_term_id', 'BIGINT NULL COMMENT ''关联学期''');
CALL phase1_add_column('student', 'school_id', 'BIGINT NULL COMMENT ''所属学校''');
CALL phase1_add_column('student', 'department_id', 'BIGINT NULL COMMENT ''所属系部''');
CALL phase1_add_column('student', 'major_id', 'BIGINT NULL COMMENT ''所属专业''');
CALL phase1_add_column('student', 'enrollment_year', 'SMALLINT NULL COMMENT ''入学年份''');
CALL phase1_add_column('student', 'student_status', 'VARCHAR(30) NULL COMMENT ''学籍状态''');
CALL phase1_add_column('student', 'education_level', 'VARCHAR(30) NULL COMMENT ''学历层次''');
CALL phase1_add_column('student', 'political_status', 'VARCHAR(30) NULL COMMENT ''政治面貌''');
CALL phase1_add_column('student', 'graduation_year', 'SMALLINT NULL COMMENT ''预计毕业年份''');
CALL phase1_add_column('student', 'profile_version', 'INT NOT NULL DEFAULT 1 COMMENT ''档案版本''');
CALL phase1_add_column('teacher', 'school_id', 'BIGINT NULL COMMENT ''所属学校''');
CALL phase1_add_column('teacher', 'department_id', 'BIGINT NULL COMMENT ''所属系部''');
CALL phase1_add_column('teacher', 'teacher_type', 'VARCHAR(30) NULL COMMENT ''教师类型''');
CALL phase1_add_column('teacher', 'title', 'VARCHAR(50) NULL COMMENT ''职称''');
CALL phase1_add_column('teacher', 'employment_status', 'VARCHAR(30) NULL COMMENT ''任职状态''');
DROP PROCEDURE IF EXISTS phase1_add_column;

-- 默认角色和最小权限字典；使用业务编码保证可重复执行。
INSERT INTO sys_role (role_code, role_name, description)
VALUES
 ('ADMIN', '系统管理员', '全校系统配置与数据管理'),
 ('EDUCATION_ADMIN', '教务人员', '全校教学数据管理'),
 ('DEPARTMENT_LEADER', '系部负责人', '本系部数据管理'),
 ('COUNSELOR', '辅导员/班主任', '负责班级学生事务'),
 ('TEACHER', '任课教师', '负责授权课程教学'),
 ('PRACTICE_TEACHER', '实习指导教师', '负责指导学生实习'),
 ('STUDENT', '学生', '仅访问本人数据'),
 ('SCHOOL_LEADER', '校领导', '授权范围内只读统计')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), description = VALUES(description), status = 1, deleted = 0;

INSERT INTO sys_permission (permission_code, permission_name, resource_type)
VALUES
 ('student:read', '查看学生', 'API'),
 ('student:write', '维护学生', 'API'),
 ('score:read', '查看成绩', 'API'),
 ('score:write', '维护成绩', 'API'),
 ('score:approve', '审核成绩', 'API'),
 ('statistics:read', '查看统计', 'API'),
 ('system:manage', '系统管理', 'API')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), status = 1;

-- 保留旧 sys_user.role 字段兼容登录，同时补齐用户角色关联。
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u JOIN sys_role r ON r.role_code = u.role
WHERE u.deleted = 0;

-- 新安装/空库的默认组织；旧数据的组织归属需要由管理员按实际资料补齐。
INSERT INTO school (school_code, school_name)
SELECT 'DEFAULT', '待完善学校' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM school WHERE school_code = 'DEFAULT');
