-- 阶段六：开放接口适配层
USE student_db;

-- 幂等键登记表
CREATE TABLE IF NOT EXISTS api_idempotency (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    request_fingerprint VARCHAR(255) NOT NULL,
    response_body TEXT NULL,
    http_status INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    UNIQUE KEY uk_idem_key (idempotency_key, request_fingerprint),
    KEY idx_idem_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 幂等键';

-- 开放接口交换审计（外部系统专用，与 operation_log 分开保留期限）
CREATE TABLE IF NOT EXISTS api_exchange_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id VARCHAR(64) NOT NULL COMMENT '服务身份标识',
    api_version VARCHAR(10) NOT NULL DEFAULT 'v1',
    request_method VARCHAR(16) NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(128) NULL,
    http_status INT NULL,
    rejected_reason VARCHAR(100) NULL COMMENT 'RATE_LIMITED/BAD_TOKEN/IDEMPOTENT_CONFLICT等',
    request_id VARCHAR(64) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ae_client (client_id, create_time),
    KEY idx_ae_rejected (rejected_reason)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放接口交换审计';
