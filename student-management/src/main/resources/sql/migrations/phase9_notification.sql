-- =============================================================
-- phase9_notification.sql — 通知中心（P2-D）
-- 幂等：CREATE TABLE IF NOT EXISTS，可重复执行；本脚本不删除任何数据表/数据
-- 表：sys_notification 用户通知（待办红点/通知面板）
-- =============================================================

CREATE TABLE IF NOT EXISTS sys_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    receiver_id BIGINT NOT NULL COMMENT '接收用户ID',
    title VARCHAR(100) NOT NULL COMMENT '标题',
    content VARCHAR(500) NOT NULL COMMENT '内容',
    type VARCHAR(20) NOT NULL DEFAULT 'SYSTEM' COMMENT '类型：LEAVE-请假 SCORE-成绩 FUND-资助 WARNING-预警 PRACTICE-实习 SYSTEM-系统',
    related_id BIGINT NULL COMMENT '关联业务ID（跳转用）',
    is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读 1-已读',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_notification_receiver (receiver_id),
    KEY idx_notification_receiver_read (receiver_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知';