-- 重置所有用户密码为 123456
-- 注意：这是通过 Java BCryptPasswordEncoder 生成的正确哈希值
-- 如果直接执行此 SQL 后仍有问题，请使用 DataInitializer 方式（重启后端）

USE student_db;

-- 方法1：如果你能重启后端，直接重启即可，DataInitializer 会自动修复

-- 方法2：手动更新（使用项目生成的正确哈希值）
-- 先不执行，等后端启动后自动生成的才是最准确的
SELECT username, role, real_name FROM sys_user;
