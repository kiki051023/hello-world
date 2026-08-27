-- ============================================================
-- 阶段七：用户 / 角色管理端（管理后台）迁移脚本
--
-- 说明：
--   1. 复用既有 RBAC 五张表（sys_role / sys_permission / sys_user_role /
--      sys_role_permission / sys_data_scope），不新建表
--   2. 补充管理类权限字典（system:user:* / system:role:*），供角色分配界面使用
--   3. 将内置 ADMIN 角色预置为全部管理权限（幂等）
--   4. 可重复执行：全部基于业务编码（role_code / permission_code）UPSERT，不删业务数据
-- ============================================================

CREATE DATABASE IF NOT EXISTS student_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE student_db;

-- ------------------------------------------------------------
-- 1. 管理类权限字典（若已存在则只更新名称/状态，不重复插入）
-- ------------------------------------------------------------
INSERT INTO sys_permission (permission_code, permission_name, resource_type)
VALUES
 ('system:user:read',    '查看用户', 'API'),
 ('system:user:write',   '新增用户', 'API'),
 ('system:user:update',  '修改用户', 'API'),
 ('system:user:reset-password', '重置密码', 'API'),
 ('system:user:disable', '禁用用户', 'API'),
 ('system:role:read',    '查看角色', 'API'),
 ('system:role:write',   '新增角色', 'API'),
 ('system:role:update',  '修改角色', 'API'),
 ('system:role:assign-permission', '分配角色权限', 'API'),
 ('system:role:assign-data-scope', '分配数据权限', 'API')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), status = 1;

-- ------------------------------------------------------------
-- 2. 为内置 ADMIN 角色预置全部管理权限（幂等）
--    ADMIN 是系统内置角色，直接以 role_code 定位，不依赖角色 ID
-- ------------------------------------------------------------
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p
  ON p.permission_code IN (
      'system:user:read', 'system:user:write', 'system:user:update',
      'system:user:reset-password', 'system:user:disable',
      'system:role:read', 'system:role:write', 'system:role:update',
      'system:role:assign-permission', 'system:role:assign-data-scope'
  )
WHERE r.role_code = 'ADMIN' AND r.status = 1 AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 清理：为角色管理预置权限时若 ADMIN 被误改，恢复其全部管理权限（幂等保险）
UPDATE sys_role SET status = 1, deleted = 0 WHERE role_code = 'ADMIN';