-- =============================================================
-- phase11_dict.sql — 数据字典（M2-1）
-- 幂等：CREATE TABLE IF NOT EXISTS + INSERT ... SELECT WHERE NOT EXISTS，可重复执行
-- 表：sys_dict_type 字典类型 / sys_dict_data 字典数据
-- 首批：性别/学历/奖惩类型/请假类型/资助类型/预警级别/就业状态/成绩状态/实习日志状态
-- =============================================================

CREATE TABLE IF NOT EXISTS sys_dict_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    dict_type VARCHAR(50) NOT NULL COMMENT '字典类型标识（如 gender）',
    dict_name VARCHAR(100) NOT NULL COMMENT '字典名称（展示用）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-启用',
    remark VARCHAR(255) NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型';

CREATE TABLE IF NOT EXISTS sys_dict_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    dict_type VARCHAR(50) NOT NULL COMMENT '字典类型标识',
    dict_value VARCHAR(50) NOT NULL COMMENT '字典值（存储值）',
    dict_label VARCHAR(100) NOT NULL COMMENT '字典标签（显示值）',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序（升序）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-启用',
    remark VARCHAR(255) NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_dict_type_value (dict_type, dict_value),
    KEY idx_dict_data_type (dict_type, status, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据';

-- ---------- 类型种子 ----------
INSERT INTO sys_dict_type (dict_type, dict_name, remark)
SELECT t.dict_type, t.dict_name, t.remark FROM (
    SELECT 'gender'            AS dict_type, '性别'        AS dict_name, '学生/教师性别'        AS remark
    UNION ALL SELECT 'education',        '学历',        '教师学历'
    UNION ALL SELECT 'reward_type',      '奖惩类型',    '奖励/惩罚分类'
    UNION ALL SELECT 'leave_type',       '请假类型',    '事假/病假等'
    UNION ALL SELECT 'funding_type',     '资助类型',    '助学金/奖学金等'
    UNION ALL SELECT 'warning_level',    '预警级别',    '学业预警等级'
    UNION ALL SELECT 'employment_status','就业状态',    '毕业生就业去向'
    UNION ALL SELECT 'score_status',     '成绩状态',    '成绩审核生命周期'
    UNION ALL SELECT 'practice_log_status','实习日志状态','实习日志审核状态'
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type d WHERE d.dict_type = t.dict_type);

-- ---------- 数据种子 ----------
INSERT INTO sys_dict_data (dict_type, dict_value, dict_label, sort)
SELECT s.* FROM (
    SELECT 'gender' AS dict_type, 'M' AS dict_value, '男' AS dict_label, 1 AS sort
    UNION ALL SELECT 'gender', 'F', '女', 2
    UNION ALL SELECT 'education', 'BACHELOR', '本科', 1
    UNION ALL SELECT 'education', 'MASTER',   '硕士', 2
    UNION ALL SELECT 'education', 'DOCTOR',   '博士', 3
    UNION ALL SELECT 'education', 'OTHER',    '其他', 4
    UNION ALL SELECT 'reward_type', 'REWARD',     '奖励', 1
    UNION ALL SELECT 'reward_type', 'PUNISHMENT', '惩罚', 2
    UNION ALL SELECT 'leave_type', 'PERSONAL', '事假', 1
    UNION ALL SELECT 'leave_type', 'SICK',     '病假', 2
    UNION ALL SELECT 'leave_type', 'OFFICIAL', '公假', 3
    UNION ALL SELECT 'leave_type', 'OTHER',    '其他', 4
    UNION ALL SELECT 'funding_type', 'GRANT',       '助学金', 1
    UNION ALL SELECT 'funding_type', 'SCHOLARSHIP', '奖学金', 2
    UNION ALL SELECT 'funding_type', 'SUBSIDY',     '困难补助', 3
    UNION ALL SELECT 'warning_level', 'YELLOW', '黄色预警', 1
    UNION ALL SELECT 'warning_level', 'ORANGE', '橙色预警', 2
    UNION ALL SELECT 'warning_level', 'RED',    '红色预警', 3
    UNION ALL SELECT 'employment_status', 'EMPLOYED',  '已就业', 1
    UNION ALL SELECT 'employment_status', 'UNEMPLOYED','待就业', 2
    UNION ALL SELECT 'employment_status', 'FURTHER',   '升学',   3
    UNION ALL SELECT 'employment_status', 'OTHER',     '其他',   4
    UNION ALL SELECT 'score_status', 'DRAFT',     '草稿',   1
    UNION ALL SELECT 'score_status', 'SUBMITTED', '已提交', 2
    UNION ALL SELECT 'score_status', 'APPROVED',  '已审核', 3
    UNION ALL SELECT 'score_status', 'REJECTED',  '已驳回', 4
    UNION ALL SELECT 'score_status', 'VOID',      '已作废', 5
    UNION ALL SELECT 'practice_log_status', 'DRAFT',     '草稿',   1
    UNION ALL SELECT 'practice_log_status', 'SUBMITTED', '已提交', 2
    UNION ALL SELECT 'practice_log_status', 'COMMENTED', '已点评', 3
) s
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data d WHERE d.dict_type = s.dict_type AND d.dict_value = s.dict_value
);
