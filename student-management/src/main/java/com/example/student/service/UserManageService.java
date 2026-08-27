package com.example.student.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.dto.UserCreateDTO;
import com.example.student.dto.UserDataScopeDTO;
import com.example.student.dto.UserUpdateDTO;
import com.example.student.entity.Role;
import com.example.student.entity.User;

import java.util.List;

/**
 * 用户管理（仅 ADMIN）：用户 CRUD、重置密码、启禁用、角色与数据权限分配。
 */
public interface UserManageService {

    IPage<User> pageUsers(long current, long size, String keyword, String role, Integer status);

    User getUserById(Long id);

    /** 创建用户：校验用户名唯一、密码强度、角色存在性；写入 sys_user + sys_user_role + sys_data_scope(user级)。 */
    void createUser(UserCreateDTO dto);

    /** 更新基本信息。 */
    void updateUser(UserUpdateDTO dto);

    /** 重置密码为初始密码（BCrypt 加密）。 */
    void resetPassword(Long id, String newPassword);

    /** 启用/禁用；禁用时立即吊销刷新令牌并使会话失效。 */
    void changeStatus(Long id, Integer status);

    /** 调整用户的角色列表；变更后 permissionVersion+1 使旧会话失效。 */
    void assignRoles(Long userId, List<Long> roleIds);

    /** 调整用户级数据权限；变更后 permissionVersion+1。 */
    void assignDataScopes(Long userId, List<UserDataScopeDTO> scopes);

    /** 查询用户已绑定角色。 */
    List<Role> rolesOfUser(Long userId);

    /** 内置角色编码集合（不可删改）。 */
    java.util.Set<String> builtinRoleCodes();
}