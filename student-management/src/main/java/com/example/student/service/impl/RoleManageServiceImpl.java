package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.dto.RoleDTO;
import com.example.student.dto.UserDataScopeDTO;
import com.example.student.entity.DataScope;
import com.example.student.entity.Permission;
import com.example.student.entity.Role;
import com.example.student.entity.RolePermission;
import com.example.student.entity.UserRole;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.DataScopeMapper;
import com.example.student.mapper.PermissionMapper;
import com.example.student.mapper.RoleMapper;
import com.example.student.mapper.RolePermissionMapper;
import com.example.student.mapper.UserMapper;
import com.example.student.mapper.UserRoleMapper;
import com.example.student.security.RedisTokenService;
import com.example.student.service.RoleManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 角色管理实现（仅 ADMIN 调用）。
 *
 * 关键设计：
 * - 内置角色（8 个固定编码）只允许改名称/描述，编码与状态锁定、禁止删除
 * - 分配权限/数据范围后，使所有持有该角色的用户会话失效（permissionVersion+1）
 * - 角色-权限（sys_role_permission）、用户-角色（sys_user_role）用专用 Mapper
 */
@Service
public class RoleManageServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleManageService {

    private static final Set<String> BUILTIN_ROLES = Set.of(
            "ADMIN", "EDUCATION_ADMIN", "DEPARTMENT_LEADER", "COUNSELOR",
            "TEACHER", "PRACTICE_TEACHER", "STUDENT", "SCHOOL_LEADER"
    );

    @Autowired
    private PermissionMapper permissionMapper;
    @Autowired
    private RolePermissionMapper rolePermissionMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private DataScopeMapper dataScopeMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RedisTokenService redisTokenService;

    @Override
    public IPage<Role> pageRoles(long current, long size, String keyword) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Role::getRoleName, keyword).or().like(Role::getRoleCode, keyword));
        }
        wrapper.orderByAsc(Role::getId);
        return page(new Page<>(current, size), wrapper);
    }

    @Override
    public Role getRoleById(Long id) {
        Role role = getById(id);
        if (role == null) throw new BusinessException("角色不存在");
        return role;
    }

    @Override
    public List<Role> listAll() {
        return list(new LambdaQueryWrapper<Role>()
                .eq(Role::getStatus, 1)
                .orderByAsc(Role::getId));
    }

    @Override
    public List<Permission> listAllPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                .eq(Permission::getStatus, 1)
                .orderByAsc(Permission::getId));
    }

    @Override
    @Transactional
    public void createRole(RoleDTO dto) {
        String code = dto.getRoleCode().toUpperCase().trim();
        if (getByCode(code) != null) {
            throw new BusinessException("角色编码已存在");
        }
        Role role = new Role();
        role.setRoleCode(code);
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        role.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        role.setDeleted(0);
        save(role);
        if (dto.getPermissionIds() != null && !dto.getPermissionIds().isEmpty()) {
            bindPermissions(role.getId(), dto.getPermissionIds());
        }
        if (dto.getDataScopes() != null && !dto.getDataScopes().isEmpty()) {
            bindDataScopes(role.getId(), dto.getDataScopes());
        }
    }

    @Override
    @Transactional
    public void updateRole(RoleDTO dto) {
        Role role = getById(dto.getId());
        if (role == null) throw new BusinessException("角色不存在");
        boolean builtin = BUILTIN_ROLES.contains(role.getRoleCode());
        // 内置角色：编码/状态锁定
        if (!builtin) {
            String newCode = dto.getRoleCode() == null ? null : dto.getRoleCode().toUpperCase().trim();
            if (newCode != null && !newCode.equals(role.getRoleCode())) {
                if (getByCode(newCode) != null) throw new BusinessException("角色编码已存在");
                role.setRoleCode(newCode);
            }
            if (dto.getStatus() != null) {
                role.setStatus(dto.getStatus());
            }
        }
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        updateById(role);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = getById(id);
        if (role == null) throw new BusinessException("角色不存在");
        if (BUILTIN_ROLES.contains(role.getRoleCode())) {
            throw new BusinessException("内置角色禁止删除");
        }
        remove(new LambdaQueryWrapper<Role>().eq(Role::getId, id));
        // 清关联（复合主键表无单一 @TableId，用 wrapper 条件删除最稳妥）
        userRoleMapper.delete(new QueryWrapper<UserRole>().eq("role_id", id));
        rolePermissionMapper.delete(new QueryWrapper<RolePermission>().eq("role_id", id));
        dataScopeMapper.delete(new QueryWrapper<DataScope>().eq("role_id", id));
        invalidateRoleSessions(id);
    }

    @Override
    public List<Permission> permissionsOfRole(Long roleId) {
        return baseMapper.selectPermissionsByRoleId(roleId);
    }

    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        Role role = getById(roleId);
        if (role == null) throw new BusinessException("角色不存在");
        // 校验权限存在
        if (permissionIds != null) {
            for (Long pid : permissionIds) {
                if (permissionMapper.selectById(pid) == null) {
                    throw new BusinessException("权限不存在: " + pid);
                }
            }
        }
        rolePermissionMapper.delete(new QueryWrapper<RolePermission>().eq("role_id", roleId));
        if (permissionIds != null && !permissionIds.isEmpty()) {
            bindPermissions(roleId, permissionIds);
        }
        invalidateRoleSessions(roleId);
    }

    @Override
    public List<DataScope> dataScopesOfRole(Long roleId) {
        return baseMapper.selectDataScopesByRoleId(roleId);
    }

    @Override
    @Transactional
    public void assignDataScopes(Long roleId, List<UserDataScopeDTO> scopes) {
        Role role = getById(roleId);
        if (role == null) throw new BusinessException("角色不存在");
        dataScopeMapper.delete(new QueryWrapper<DataScope>().eq("role_id", roleId));
        if (scopes != null && !scopes.isEmpty()) {
            bindDataScopes(roleId, scopes);
        }
        invalidateRoleSessions(roleId);
    }

    @Override
    public Set<String> builtinRoleCodes() {
        return BUILTIN_ROLES;
    }

    // ---------- 私有 ----------

    private Role getByCode(String code) {
        List<Role> roles = baseMapper.selectList(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleCode, code));
        return roles == null || roles.isEmpty() ? null : roles.get(0);
    }

    private void bindPermissions(Long roleId, List<Long> permissionIds) {
        for (Long pid : permissionIds) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(pid);
            rolePermissionMapper.insert(rp);
        }
    }

    private void bindDataScopes(Long roleId, List<UserDataScopeDTO> scopes) {
        for (UserDataScopeDTO s : scopes) {
            if (s.getScopeType() == null || s.getScopeType().isBlank()) continue;
            DataScope ds = new DataScope();
            ds.setRoleId(roleId);
            ds.setScopeType(s.getScopeType());
            ds.setScopeValue(s.getScopeValue());
            dataScopeMapper.insert(ds);
        }
    }

    /** 使所有持有该角色的用户旧会话失效。 */
    private void invalidateRoleSessions(Long roleId) {
        List<UserRole> urs = userRoleMapper.selectList(new QueryWrapper<UserRole>().eq("role_id", roleId));
        for (UserRole ur : urs) {
            com.example.student.entity.User u = userMapper.selectById(ur.getUserId());
            if (u == null) continue;
            int v = (u.getPermissionVersion() == null ? 1 : u.getPermissionVersion()) + 1;
            u.setPermissionVersion(v);
            userMapper.updateById(u);
            redisTokenService.storePermissionVersion(u.getId(), v);
        }
    }
}