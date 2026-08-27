package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.dto.UserCreateDTO;
import com.example.student.dto.UserDataScopeDTO;
import com.example.student.dto.UserUpdateDTO;
import com.example.student.entity.DataScope;
import com.example.student.entity.Role;
import com.example.student.entity.User;
import com.example.student.entity.UserRole;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.DataScopeMapper;
import com.example.student.mapper.RoleMapper;
import com.example.student.mapper.UserMapper;
import com.example.student.mapper.UserRoleMapper;
import com.example.student.security.RedisTokenService;
import com.example.student.service.UserManageService;
import com.example.student.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 用户管理实现（仅 ADMIN 调用）。
 *
 * 关键设计：
 * - 密码一律 BCrypt 存储，绝不明文
 * - 新建/重置密码/改权限后 permissionVersion+1，使该用户旧会话立即失效
 * - 删除仅逻辑禁用（status=0），不物理删除——保留审计与关联链路
 * - 角色（sys_user_role）与数据权限（sys_data_scope 用户级）用专用 Mapper 写，
 *   避免 RoleMapper(BaseMapper<Role>) 解析到错误表名
 */
@Service
public class UserManageServiceImpl extends ServiceImpl<UserMapper, User> implements UserManageService {

    private static final Set<String> BUILTIN_ROLES = Set.of(
            "ADMIN", "EDUCATION_ADMIN", "DEPARTMENT_LEADER", "COUNSELOR",
            "TEACHER", "PRACTICE_TEACHER", "STUDENT", "SCHOOL_LEADER"
    );

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RedisTokenService redisTokenService;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private DataScopeMapper dataScopeMapper;
    @Autowired
    private UserService userService;

    @Override
    public IPage<User> pageUsers(long current, long size, String keyword, String role, Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getRealName, keyword));
        }
        if (role != null && !role.isBlank()) {
            wrapper.eq(User::getRole, role);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getId);
        return page(new Page<>(current, size), wrapper);
    }

    @Override
    public User getUserById(Long id) {
        User user = getById(id);
        if (user == null) throw new BusinessException("用户不存在");
        return user;
    }

    @Override
    @Transactional
    public void createUser(UserCreateDTO dto) {
        // 密码强度（防御性校验：即使绕过 Controller @Valid 也拒绝弱密码）
        if (dto.getPassword() == null || dto.getPassword().length() < 8
                || !dto.getPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")) {
            throw new BusinessException("密码必须包含大写字母、小写字母和数字，且长度至少8位");
        }
        if (userService.getByUsername(dto.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        validateRoleIds(dto.getRoleIds());

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setRole(dto.getRole() == null || dto.getRole().isBlank() ? "STUDENT" : dto.getRole().toUpperCase());
        user.setSchoolId(dto.getSchoolId());
        user.setDepartmentId(dto.getDepartmentId());
        user.setEmployeeType(dto.getEmployeeType());
        user.setStatus(1);
        user.setPermissionVersion(1);
        save(user);

        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            bindRoles(user.getId(), dto.getRoleIds());
        } else {
            Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                    .eq(Role::getRoleCode, user.getRole()));
            if (role != null) {
                bindRoles(user.getId(), List.of(role.getId()));
            }
        }
        if (dto.getDataScopes() != null && !dto.getDataScopes().isEmpty()) {
            bindDataScopes(user.getId(), null, dto.getDataScopes());
        }
    }

    @Override
    @Transactional
    public void updateUser(UserUpdateDTO dto) {
        User user = getUserById(dto.getId());
        user.setRealName(dto.getRealName());
        user.setEmployeeType(dto.getEmployeeType());
        user.setSchoolId(dto.getSchoolId());
        user.setDepartmentId(dto.getDepartmentId());
        updateById(user);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        if (newPassword == null || newPassword.length() < 8
                || !newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")) {
            throw new BusinessException("密码必须包含大写字母、小写字母和数字，且长度至少8位");
        }
        User user = getUserById(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        bumpPermissionVersion(user, true);
    }

    @Override
    @Transactional
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值非法");
        }
        User user = getUserById(id);
        if (status == 0 && isSelf(id)) {
            throw new BusinessException("不能禁用当前登录的管理员账号");
        }
        user.setStatus(status);
        updateById(user);
        if (status == 0) {
            invalidateSessions(user.getId());
        }
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        User user = getUserById(userId);
        validateRoleIds(roleIds);
        // 清旧角色（复合主键表用 wrapper 条件删除）
        userRoleMapper.delete(new QueryWrapper<UserRole>().eq("user_id", userId));
        bindRoles(userId, roleIds);
        bumpPermissionVersion(user, false);
    }

    @Override
    @Transactional
    public void assignDataScopes(Long userId, List<UserDataScopeDTO> scopes) {
        User user = getUserById(userId);
        dataScopeMapper.delete(new QueryWrapper<DataScope>()
                .eq("user_id", userId).isNull("role_id"));
        if (scopes != null && !scopes.isEmpty()) {
            bindDataScopes(userId, null, scopes);
        }
        bumpPermissionVersion(user, false);
    }

    @Override
    public List<Role> rolesOfUser(Long userId) {
        return roleMapper.selectRolesByUserId(userId);
    }

    @Override
    public Set<String> builtinRoleCodes() {
        return BUILTIN_ROLES;
    }

    // ---------- 私有 ----------

    private void validateRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return;
        for (Long rid : roleIds) {
            Role role = roleMapper.selectById(rid);
            if (role == null || (role.getDeleted() != null && role.getDeleted() == 1)) {
                throw new BusinessException("角色不存在或已删除: " + rid);
            }
        }
    }

    private void bindRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return;
        for (Long rid : roleIds) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(rid);
            userRoleMapper.insert(ur);
        }
    }

    private void bindDataScopes(Long userId, Long roleId, List<UserDataScopeDTO> scopes) {
        for (UserDataScopeDTO s : scopes) {
            if (s.getScopeType() == null || s.getScopeType().isBlank()) continue;
            DataScope ds = new DataScope();
            ds.setUserId(userId);
            ds.setRoleId(roleId);
            ds.setScopeType(s.getScopeType());
            ds.setScopeValue(s.getScopeValue());
            dataScopeMapper.insert(ds);
        }
    }

    /** 权限版本 +1：使该用户所有旧访问令牌（携带旧版本）被 JwtAuthenticationFilter 拒绝。 */
    private void bumpPermissionVersion(User user, boolean alsoRevokeRefresh) {
        user.setPermissionVersion(user.getPermissionVersion() == null ? 1 : user.getPermissionVersion() + 1);
        updateById(user);
        redisTokenService.storePermissionVersion(user.getId(), user.getPermissionVersion());
        if (alsoRevokeRefresh) {
            redisTokenService.removeRefreshToken(user.getId());
        }
    }

    private void invalidateSessions(Long userId) {
        // 禁用：吊销刷新令牌。访问令牌由 permissionVersion 一致性间接失效，
        // 若担心黑名单即时性，可另做 access 黑名单（本实现依赖版本机制）。
        redisTokenService.removeRefreshToken(userId);
    }

    private boolean isSelf(Long id) {
        if (id == null) return false;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return false;
        User u = userService.getByUsername(auth.getName());
        return u != null && u.getId().equals(id);
    }
}