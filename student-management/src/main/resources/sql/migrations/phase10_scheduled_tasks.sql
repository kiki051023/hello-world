-- =============================================================
-- phase10_scheduled_tasks.sql — 定时任务框架（M1-1）
-- 幂等：CREATE TABLE IF NOT EXISTS / INSERT IGNORE，可重复执行；本脚本不删除任何数据表/数据
-- 表：sys_scheduled_task 任务配置（代码里由 SchedulingConfigurer 读取 cron+enabled 注册）
--     sys_scheduled_task_log 任务执行日志（每次执行一行，成功/失败均记录）
-- 内置 4 个首批任务（预警扫描 / 学期切换 / 通知清理 / 导入超时回收）
-- =============================================================

CREATE TABLE IF NOT EXISTS sys_scheduled_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_code VARCHAR(50) NOT NULL COMMENT '任务编码',
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    cron VARCHAR(50) NOT NULL COMMENT 'cron 表达式（6段：秒 分 时 日 月 周）',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-停用 1-启用',
    remark VARCHAR(200) NULL COMMENT '说明',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_scheduled_task_code (task_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务配置';

CREATE TABLE IF NOT EXISTS sys_scheduled_task_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_code VARCHAR(50) NOT NULL COMMENT '任务编码',
    task_name VARCHAR(100) NULL COMMENT '任务名称（冗余快照）',
    status VARCHAR(10) NOT NULL COMMENT '执行结果：SUCCESS/FAILED',
    error_message VARCHAR(500) NULL COMMENT '失败原因（截断）',
    cost_ms BIGINT NOT NULL DEFAULT 0 COMMENT '耗时（毫秒）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
    KEY idx_task_log_code_time (task_code, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务执行日志';

INSERT IGNORE INTO sys_scheduled_task (task_code, task_name, cron, enabled, remark) VALUES
('WARNING_SCAN',     '学业预警扫描',   '0 0 6 * * ?',   1, '每日 06:00 扫描挂科学生生成学业预警（幂等，已有 ACTIVE 同键不重复）'),
('TERM_AUTO_SWITCH', '学期自动切换',   '0 30 0 * * ?',  1, '每日 00:30 按起止日期自动切换当前学期'),
('NOTIFY_CLEANUP',   '已读通知清理',   '0 0 2 * * ?',   1, '每日 02:00 清理 30 天前已读通知'),
('IMPORT_REAPER',    '导入任务超时回收', '0 */30 * * * ?', 1, '每 30 分钟回收 RUNNING 且超时 30 分钟的导入任务为 FAILED');