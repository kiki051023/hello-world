-- =============================================================
-- phase14_timetable.sql — 课表时间槽（M4-2）
-- 幂等：information_schema 守护的 ALTER
-- teaching_task 增加 week_day(1-7)/period(节次)/classroom 供周课表与冲突检测
-- =============================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS phase14_add_col $$
CREATE PROCEDURE phase14_add_col()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teaching_task' AND COLUMN_NAME = 'week_day') THEN
        ALTER TABLE teaching_task ADD COLUMN week_day TINYINT NULL COMMENT '星期几 1-7';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teaching_task' AND COLUMN_NAME = 'period') THEN
        ALTER TABLE teaching_task ADD COLUMN period VARCHAR(20) NULL COMMENT '节次 如 1-2';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teaching_task' AND COLUMN_NAME = 'classroom') THEN
        ALTER TABLE teaching_task ADD COLUMN classroom VARCHAR(50) NULL COMMENT '教室';
    END IF;
END $$
CALL phase14_add_col() $$
DROP PROCEDURE phase14_add_col $$

DELIMITER ;
