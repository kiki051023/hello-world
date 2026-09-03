-- =============================================================
-- phase15_menu.sql — 动态菜单（M4-6）
-- 幂等：CREATE TABLE IF NOT EXISTS + INSERT WHERE NOT EXISTS
-- sys_menu：path 唯一；roles 存逗号分隔角色码（与前端 meta.roles 同构，不做角色绑定表——YAGNI）
-- 种子 = Layout.vue allMenus 全量 21 项
-- =============================================================

CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单ID（0=顶级）',
    title VARCHAR(50) NOT NULL COMMENT '菜单标题',
    path VARCHAR(100) NOT NULL COMMENT '路由路径',
    icon VARCHAR(50) NULL COMMENT '图标名（Element Plus icons）',
    roles VARCHAR(200) NOT NULL COMMENT '可见角色码，逗号分隔',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_menu_path (path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态菜单';

INSERT INTO sys_menu (title, path, icon, roles, sort)
SELECT s.* FROM (
    SELECT '首页'     AS title, '/dashboard'      AS path, 'HomeFilled'      AS icon, 'ADMIN,TEACHER,STUDENT' AS roles, 1 AS sort
    UNION ALL SELECT '成绩分析','/analysis','TrendCharts','ADMIN,TEACHER,STUDENT',2
    UNION ALL SELECT '我的门户','/portal','User','STUDENT',3
    UNION ALL SELECT '成绩管理','/score','DataAnalysis','ADMIN,TEACHER,STUDENT',10
    UNION ALL SELECT '学期管理','/system/terms','Calendar','ADMIN,EDUCATION_ADMIN',11
    UNION ALL SELECT '成绩审核','/score-audit','Checked','ADMIN,EDUCATION_ADMIN,DEPARTMENT_LEADER,TEACHER',12
    UNION ALL SELECT '教务管理','/education','Calendar','ADMIN,EDUCATION_ADMIN,DEPARTMENT_LEADER,COUNSELOR,TEACHER,STUDENT',13
    UNION ALL SELECT '我的课表','/timetable','Calendar','ADMIN,EDUCATION_ADMIN,DEPARTMENT_LEADER,COUNSELOR,TEACHER,STUDENT',14
    UNION ALL SELECT '学生管理','/student','User','ADMIN,TEACHER',20
    UNION ALL SELECT '班级管理','/clazz','OfficeBuilding','ADMIN,TEACHER',21
    UNION ALL SELECT '课程管理','/course','Reading','ADMIN,TEACHER',22
    UNION ALL SELECT '教师管理','/teacher','Avatar','ADMIN',23
    UNION ALL SELECT '学生事务','/affairs','Notebook','ADMIN,EDUCATION_ADMIN,DEPARTMENT_LEADER,COUNSELOR,TEACHER,STUDENT',24
    UNION ALL SELECT '实习就业','/practice','Suitcase','ADMIN,EDUCATION_ADMIN,DEPARTMENT_LEADER,COUNSELOR,TEACHER,PRACTICE_TEACHER,STUDENT',25
    UNION ALL SELECT '运维监控','/ops','Monitor','ADMIN',30
    UNION ALL SELECT '组织架构','/system/org','OfficeBuilding','ADMIN,EDUCATION_ADMIN,DEPARTMENT_LEADER,COUNSELOR',31
    UNION ALL SELECT '用户管理','/system/users','User','ADMIN',32
    UNION ALL SELECT '角色管理','/system/roles','Avatar','ADMIN',33
    UNION ALL SELECT '字典管理','/system/dict','Collection','ADMIN',34
    UNION ALL SELECT '宿舍管理','/system/dorm','House','ADMIN',35
) s
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.path = s.path);
