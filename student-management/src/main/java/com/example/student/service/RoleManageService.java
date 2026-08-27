package com.example.student.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.dto.RoleDTO;
import com.example.student.dto.UserDataScopeDTO;
import com.example.student.entity.DataScope;
import com.example.student.entity.Permission;
import com.example.student.entity.Role;

import java.util.List;

/**
 * 角色管理（仅 ADMIN）：角色 CRUD、权限分配、数据范围模板。
 */
public interface RoleManageService {

    IPage<Role> pageRoles(long current, long size, String keyword);

    Role getRoleById(Long id);

    List<Role> listAll();

    List<Permission> listAllPermissions();

    /** 新建角色；校验编码唯一。 */
    void createRole(RoleDTO dto);

    /** 编辑角色；内置角色只允许改名称/描述，编码与状态锁定。 */
    void updateRole(RoleDTO dto);

    /** 删除角色（逻辑删除）；内置角色禁止删除。 */
    void deleteRole(Long id);

    /** 查询角色已拥权限。 */
    List<Permission> permissionsOfRole(Long roleId);

    /** 分配角色权限；保存后使相关用户会话失效。 */
    void assignPermissions(Long roleId, List<Long> permissionIds);

    /** 查询角色数据范围模板。 */
    List<DataScope> dataScopesOfRole(Long roleId);

    /** 分配角色数据范围模板。 */
    void assignDataScopes(Long roleId, List<UserDataScopeDTO> scopes);

    /** 内置角色编码。 */
    java.util.Set<String> builtinRoleCodes();
}