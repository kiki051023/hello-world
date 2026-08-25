-- 阶段三：学生事务迁移（MySQL 8.0+）
--
-- 用法：在已完成阶段一/二迁移的 student_db 上直接执行。
-- 幂等：重复执行不会删除业务数据，也不会重复创建列、表或索引。

USE student_db;

-- ============ Task 8: 学籍与学生档案 ============

CREATE TABLE IF NOT EXISTS student_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL COMMENT '学生ID',
    change_type VARCHAR(30) NOT NULL COMMENT 'STATUS/TRANSFER_MAJOR/TRANSFER_CLASS/SUSPEND/RESUME/GRADUATE',
    old_value VARCHAR(100) NULL,
    new_value VARCHAR(100) NULL,
    reason VARCHAR(255) NULL,
    operator_id BIGINT NULL,
    operator_name VARCHAR(100) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ssh_student (student_id),
    KEY idx_ssh_created (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学籍异动历史（只追加）';

-- ============ Task 9: 请假、奖惩、资助和预警 ============

CREATE TABLE IF NOT EXISTS leave_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    leave_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL' COMMENT 'PERSONAL事假/SICK病假/OTHER其他',
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    reason VARCHAR(500) NULL,
    destination VARCHAR(200) NULL COMMENT '去向',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED/RETURNED已销假/OVERDUE超时未销假',
    approver_id BIGINT NULL,
    approver_name VARCHAR(100) NULL,
    approve_comment VARCHAR(255) NULL,
    approved_at DATETIME NULL,
    returned_at DATETIME NULL COMMENT '销假时间',
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_leave_student (student_id),
    KEY idx_leave_status (status),
    KEY idx_leave_range (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生请假';

CREATE TABLE IF NOT EXISTS reward_punishment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    rp_type VARCHAR(10) NOT NULL COMMENT 'REWARD奖励/PUNISHMENT处分',
    level_code VARCHAR(30) NOT NULL COMMENT '国家级/省级/校级/院级 或 警告/严重警告/记过/留校察看/开除学籍',
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000) NULL,
    awarded_date DATE NULL,
    document_no VARCHAR(100) NULL COMMENT '文号',
    revoke_date DATE NULL COMMENT '处分解除日期',
    revoked TINYINT NOT NULL DEFAULT 0 COMMENT '处分是否已解除',
    operator_id BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_rp_student (student_id),
    KEY idx_rp_type (rp_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖惩记录';

CREATE TABLE IF NOT EXISTS hardship_funding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    academic_year VARCHAR(20) NOT NULL COMMENT '学年，如2024-2025',
    funding_type VARCHAR(30) NOT NULL COMMENT 'GRANT助学金/SCHOLARSHIP奖学金/LOAN助学贷款/WAIVER减免',
    amount DECIMAL(10,2) NULL,
    difficulty_level VARCHAR(20) NULL COMMENT '特别困难/比较困难/一般困难',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/DISBURSED已发放',
    reviewer_id BIGINT NULL,
    review_comment VARCHAR(255) NULL,
    sensitive_note VARCHAR(500) NULL COMMENT '敏感备注：独立字段，仅授权角色可见',
    operator_id BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_funding_once (student_id, academic_year, funding_type),
    KEY idx_funding_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='困难资助（敏感数据表）';

CREATE TABLE IF NOT EXISTS student_warning (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    warning_type VARCHAR(20) NOT NULL COMMENT 'SCORE成绩/LEAVE请假/RP违纪/FUNDING资助',
    rule_key VARCHAR(50) NOT NULL COMMENT '规则键，如 score_fail_3 / leave_overdue_2',
    rule_threshold INT NOT NULL DEFAULT 1 COMMENT '触发阈值',
    current_value INT NOT NULL DEFAULT 0 COMMENT '触发时实际值',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/HANDLED/DISMISSED',
    handled_by BIGINT NULL,
    handle_note VARCHAR(255) NULL,
    handled_at DATETIME NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_warning_active (student_id, warning_type, rule_key, status),
    KEY idx_warning_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生预警';
