-- =============================================================
-- phase12_dorm.sql — 宿舍管理示例（M2-3 生成器端到端验证模块）
-- 幂等：CREATE TABLE IF NOT EXISTS，可重复执行
-- =============================================================

CREATE TABLE IF NOT EXISTS dorm_room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    building VARCHAR(50) NOT NULL COMMENT '楼栋',
    room_no VARCHAR(20) NOT NULL COMMENT '房间号',
    capacity INT NOT NULL DEFAULT 4 COMMENT '容量',
    remark VARCHAR(255) NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_dorm_room (building, room_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宿舍房间';
