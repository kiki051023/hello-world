-- 异步导出任务表（阶段五遗留项：超阈值导出转异步）
USE student_db;

CREATE TABLE IF NOT EXISTS export_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_no VARCHAR(64) NOT NULL,
    business_type VARCHAR(30) NOT NULL,
    params_json VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
    total_rows INT NULL,
    requester_id BIGINT NULL,
    result_key VARCHAR(128) NULL COMMENT '完成后结果对象 key',
    error_message VARCHAR(500) NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_export_task_no (task_no),
    KEY idx_export_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步导出任务';
