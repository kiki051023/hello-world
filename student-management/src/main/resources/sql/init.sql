-- ============================================
-- 学生管理系统 - 数据库初始化脚本
-- ============================================

CREATE DATABASE IF NOT EXISTS student_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE student_db;

-- ============================================
-- 1. 用户表
-- ============================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name VARCHAR(50) COMMENT '真实姓名',
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT '角色：ADMIN-管理员 TEACHER-教师 STUDENT-学生',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 2. 班级表
-- ============================================
DROP TABLE IF EXISTS clazz;
CREATE TABLE clazz (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    clazz_name VARCHAR(50) NOT NULL COMMENT '班级名称',
    grade VARCHAR(20) COMMENT '年级',
    head_teacher_id BIGINT COMMENT '班主任ID',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- ============================================
-- 3. 教师表
-- ============================================
DROP TABLE IF EXISTS teacher;
CREATE TABLE teacher (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    teacher_no VARCHAR(20) NOT NULL UNIQUE COMMENT '工号',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender CHAR(1) COMMENT '性别：男/女',
    phone VARCHAR(20) COMMENT '联系电话',
    user_id BIGINT COMMENT '关联用户ID',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_teacher_no (teacher_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师表';

-- ============================================
-- 4. 学生表
-- ============================================
DROP TABLE IF EXISTS student;
CREATE TABLE student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    student_no VARCHAR(20) NOT NULL UNIQUE COMMENT '学号',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender CHAR(1) COMMENT '性别：男/女',
    birthday DATE COMMENT '出生日期',
    phone VARCHAR(20) COMMENT '联系电话',
    address VARCHAR(200) COMMENT '家庭住址',
    clazz_id BIGINT COMMENT '班级ID',
    user_id BIGINT COMMENT '关联用户ID',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_student_no (student_no),
    INDEX idx_clazz_id (clazz_id),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生表';

-- ============================================
-- 5. 课程表
-- ============================================
DROP TABLE IF EXISTS course;
CREATE TABLE course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    course_no VARCHAR(20) NOT NULL UNIQUE COMMENT '课程编号',
    course_name VARCHAR(50) NOT NULL COMMENT '课程名称',
    credit INT DEFAULT 0 COMMENT '学分',
    teacher_id BIGINT COMMENT '授课教师ID',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_course_no (course_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- ============================================
-- 6. 成绩表
-- ============================================
DROP TABLE IF EXISTS score;
CREATE TABLE score (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    score DECIMAL(5,2) COMMENT '分数',
    exam_time DATE COMMENT '考试时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_student_id (student_id),
    INDEX idx_course_id (course_id),
    UNIQUE KEY uk_student_course (student_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩表';

-- ============================================
-- 初始化测试数据
-- ============================================

-- 管理员用户（密码：123456，BCrypt加密后）
INSERT INTO sys_user (username, password, real_name, role, status) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 'ADMIN', 1),
('teacher1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '张老师', 'TEACHER', 1),
('student1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '张三', 'STUDENT', 1),
('student2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '李四', 'STUDENT', 1);

-- 班级
INSERT INTO clazz (clazz_name, grade, head_teacher_id) VALUES
('计算机2301班', '2023级', 1),
('计算机2302班', '2023级', 1),
('软件工程2301班', '2023级', 1);

-- 教师
INSERT INTO teacher (teacher_no, name, gender, phone, user_id) VALUES
('T001', '张老师', '男', '13800138001', 2),
('T002', '李老师', '女', '13800138002', NULL);

-- 学生
INSERT INTO student (student_no, name, gender, birthday, phone, address, clazz_id, user_id) VALUES
('2023001', '张三', '男', '2005-03-15', '13900139001', '北京市朝阳区', 1, 3),
('2023002', '李四', '女', '2005-06-20', '13900139002', '上海市浦东新区', 1, 4),
('2023003', '王五', '男', '2005-09-10', '13900139003', '广州市天河区', 2, NULL),
('2023004', '赵六', '女', '2005-12-01', '13900139004', '深圳市南山区', 2, NULL),
('2023005', '孙七', '男', '2005-02-28', '13900139005', '杭州市西湖区', 3, NULL);

-- 课程
INSERT INTO course (course_no, course_name, credit, teacher_id) VALUES
('C001', '高等数学', 4, 1),
('C002', '大学英语', 4, 2),
('C003', 'Java程序设计', 3, 1),
('C004', '数据结构', 3, 1),
('C005', '数据库原理', 3, 2);

-- 成绩
INSERT INTO score (student_id, course_id, score, exam_time) VALUES
(1, 1, 85.5, '2024-01-15'),
(1, 2, 78.0, '2024-01-16'),
(1, 3, 92.0, '2024-01-17'),
(2, 1, 90.0, '2024-01-15'),
(2, 2, 88.5, '2024-01-16'),
(2, 3, 85.0, '2024-01-17'),
(3, 1, 76.0, '2024-01-15'),
(3, 2, 82.0, '2024-01-16'),
(4, 1, 95.0, '2024-01-15'),
(4, 4, 88.0, '2024-01-18'),
(5, 1, 80.0, '2024-01-15'),
(5, 5, 91.0, '2024-01-19');
