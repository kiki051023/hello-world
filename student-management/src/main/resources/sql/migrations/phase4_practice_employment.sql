-- 阶段四：实习就业迁移（MySQL 8.0+）
--
-- 用法：在已完成阶段一/二/三迁移的 student_db 上直接执行。
-- 幂等：重复执行不会删除业务数据，也不会重复创建列、表或索引。

USE student_db;

-- ============ Task 10: 实习过程和企业管理 ============

CREATE TABLE IF NOT EXISTS practice_company (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(200) NOT NULL,
    credit_code VARCHAR(50) NULL COMMENT '统一社会信用代码',
    industry VARCHAR(50) NULL,
    contact_person VARCHAR(50) NULL,
    contact_phone VARCHAR(30) NULL,
    address VARCHAR(300) NULL,
    cooperation_status TINYINT NOT NULL DEFAULT 1 COMMENT '1合作中 0已终止',
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实习企业';

CREATE TABLE IF NOT EXISTS practice_position (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL COMMENT '实习企业ID',
    position_name VARCHAR(100) NOT NULL,
    headcount INT NOT NULL DEFAULT 0 COMMENT '招聘人数',
    salary_range VARCHAR(50) NULL,
    requirement VARCHAR(500) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_pp_company (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实习岗位';

CREATE TABLE IF NOT EXISTS practice_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year VARCHAR(20) NOT NULL,
    plan_name VARCHAR(100) NOT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_practice_plan (academic_year, plan_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实习计划';

CREATE TABLE IF NOT EXISTS practice_assignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL COMMENT '实习计划ID',
    student_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    position_id BIGINT NULL,
    supervisor_user_id BIGINT NULL COMMENT '企业导师用户ID',
    instructor_teacher_id BIGINT NOT NULL COMMENT '校内指导教师ID(teacher.id)',
    start_date DATE NULL,
    end_date DATE NULL,
    practice_score DECIMAL(5,2) NULL COMMENT '实习成绩，审核后写入',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/FINISHED/CANCELLED',
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_practice_assign (plan_id, student_id),
    KEY idx_pa_instructor (instructor_teacher_id),
    KEY idx_pa_company (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生实习分配（含指导关系）';

CREATE TABLE IF NOT EXISTS practice_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL COMMENT '实习分配ID',
    week_no INT NOT NULL COMMENT '第N周',
    content TEXT NULL COMMENT '周记内容',
    log_date DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED/COMMENTED',
    teacher_comment VARCHAR(500) NULL,
    commented_at DATETIME NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_plog_week (assignment_id, week_no),
    KEY idx_plog_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实习周记';

CREATE TABLE IF NOT EXISTS practice_evaluation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    evaluator_type VARCHAR(20) NOT NULL COMMENT 'TEACHER校内指导/COMPANY企业导师/SELF自评',
    score DECIMAL(5,2) NULL,
    comment VARCHAR(1000) NULL,
    evaluated_at DATETIME NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_peval_once (assignment_id, evaluator_type),
    KEY idx_peval_assign (assignment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实习评价';

CREATE TABLE IF NOT EXISTS attachment_object (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    object_key VARCHAR(128) NOT NULL COMMENT '对象ID（不暴露物理路径）',
    original_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NULL,
    size_bytes BIGINT NULL,
    business_type VARCHAR(30) NOT NULL COMMENT 'PRACTICE_LOG/CERTIFICATE/OTHER',
    business_id BIGINT NULL COMMENT '关联业务记录ID',
    uploader_id BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_attachment_key (object_key),
    KEY idx_att_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件对象（存对象ID不存路径）';

-- ============ Task 11: 就业与证书 ============

CREATE TABLE IF NOT EXISTS employment_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    graduation_year SMALLINT NOT NULL,
    destination_type VARCHAR(20) NOT NULL COMMENT 'EMPLOYED就业/FURTHER_STUDY升学/ENTREPRENEUR创业/MILITARY入伍/UNEMPLOYED未就业/FLEXIBLE灵活就业',
    employer_name VARCHAR(200) NULL,
    position_title VARCHAR(100) NULL,
    city VARCHAR(50) NULL,
    contract_start DATE NULL,
    confirmed TINYINT NOT NULL DEFAULT 0 COMMENT '辅导员核验',
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_employ_year (student_id, graduation_year),
    KEY idx_emp_dest (graduation_year, destination_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='就业去向';

CREATE TABLE IF NOT EXISTS student_certificate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    cert_name VARCHAR(200) NOT NULL COMMENT '证书名称',
    cert_level VARCHAR(50) NULL COMMENT '等级：初级/中级/高级/CET-4等',
    issuer VARCHAR(200) NULL COMMENT '颁发机构',
    issued_date DATE NULL,
    verify_no VARCHAR(100) NULL COMMENT '验证编号',
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_cert_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能证书';
