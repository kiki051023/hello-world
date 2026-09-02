-- =============================================================
-- phase13_message_channel.sql — 消息多渠道（M3-1）
-- 幂等：信息_schema 守护的 ALTER + CREATE TABLE IF NOT EXISTS
-- sys_user 增加 email；sys_notify_channel_log 记录外发结果供重试
-- =============================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS phase13_add_col $$
CREATE PROCEDURE phase13_add_col()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'email') THEN
        ALTER TABLE sys_user ADD COLUMN email VARCHAR(100) NULL COMMENT '邮箱（通知邮件渠道）';
    END IF;
END $$
CALL phase13_add_col() $$
DROP PROCEDURE phase13_add_col $$

DELIMITER ;

CREATE TABLE IF NOT EXISTS sys_notify_channel_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    notification_id BIGINT NOT NULL COMMENT '站内通知ID',
    receiver_id BIGINT NOT NULL COMMENT '接收用户ID',
    channel VARCHAR(20) NOT NULL COMMENT '渠道：EMAIL/WECOM',
    target VARCHAR(200) NULL COMMENT '目标（邮箱地址/群 webhook 掩码）',
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED',
    error VARCHAR(500) NULL COMMENT '失败原因',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_channel_log_status (status, retry_count),
    KEY idx_channel_log_notification (notification_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知外发渠道日志';

-- 渠道重试定时任务（M3-1，幂等）
INSERT INTO sys_scheduled_task (task_code, task_name, cron, enabled, remark)
SELECT 'NOTIFY_CHANNEL_RETRY', '通知外发重试', '0 */10 * * * ?', 1, '每 10 分钟重试 FAILED 且次数<3 的渠道外发'
WHERE NOT EXISTS (SELECT 1 FROM sys_scheduled_task t WHERE t.task_code = 'NOTIFY_CHANNEL_RETRY');
