package com.example.student.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.student.dto.RoleDTO;
import com.example.student.dto.UserDataScopeDTO;
import com.example.student.entity.Role;
import com.example.student.entity.RolePermission;
import com.example.student.entity.User;
import com.example.student.entity.UserRole;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.DataScopeMapper;
import com.example.student.mapper.PermissionMapper;
import com.example.student.mapper.RoleMapper;
import com.example.student.mapper.RolePermissionMapper;
import com.example.student.mapper.UserMapper;
import com.example.student.mapper.UserRoleMapper;
import com.example.student.security.RedisTokenService;
import com.example.student.service.impl.RoleManageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 角色管理核心边界：编码唯一、内置角色禁删、权限分配后会话失效。
 */
class RoleManageServiceTest {

    private RoleManageServiceImpl service;
    private RoleMapper roleMapper;
    private PermissionMapper permissionMapper;
    private RolePermissionMapper rolePermissionMapper;
    private UserRoleMapper userRoleMapper;
    private DataScopeMapper dataScopeMapper;
    private UserMapper userMapper;
    private RedisTokenService redisTokenService;

    @BeforeEach
    void setUp() {
        service = new RoleManageServiceImpl();
        roleMapper = mock(RoleMapper.class);
        permissionMapper = mock(PermissionMapper.class);
        rolePermissionMapper = mock(RolePermissionMapper.class);
        userRoleMapper = mock(UserRoleMapper.class);
        dataScopeMapper = mock(DataScopeMapper.class);
        userMapper = mock(UserMapper.class);
        redisTokenService = mock(RedisTokenService.class);

        ReflectionTestUtils.setField(service, "baseMapper", roleMapper);
        ReflectionTestUtils.setField(service, "permissionMapper", permissionMapper);
        ReflectionTestUtils.setField(service, "rolePermissionMapper", rolePermissionMapper);
        ReflectionTestUtils.setField(service, "userRoleMapper", userRoleMapper);
        ReflectionTestUtils.setField(service, "dataScopeMapper", dataScopeMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "redisTokenService", redisTokenService);
    }

    @Test
    void createRoleRejectsDuplicateCode() {
        // ServiceImpl.getOne(wrapper,false) 内部走 baseMapper.selectList
        when(roleMapper.selectList(any())).thenReturn(List.of(role(1L, "ADMIN")));
        RoleDTO dto = new RoleDTO();
        dto.setRoleCode("admin");
        dto.setRoleName("管理员");
        assertThrows(BusinessException.class, () -> service.createRole(dto));
        verify(roleMapper, never()).insert(any(Role.class));
    }

    @Test
    void createRoleNormalizesCodeUppercase() {
        when(roleMapper.selectList(any())).thenReturn(List.of());
        when(roleMapper.insert(any(Role.class))).thenReturn(1);

        RoleDTO dto = new RoleDTO();
        dto.setRoleCode("lab_asset");
        dto.setRoleName("实验室管理员");
        service.createRole(dto);

        org.mockito.ArgumentCaptor<Role> cap = org.mockito.ArgumentCaptor.forClass(Role.class);
        verify(roleMapper).insert(cap.capture());
        assertEquals("LAB_ASSET", cap.getValue().getRoleCode());
    }

    @Test
    void deleteBuiltinRoleForbidden() {
        when(roleMapper.selectById(1L)).thenReturn(role(1L, "ADMIN"));
        assertThrows(BusinessException.class, () -> service.deleteRole(1L));
        // 内置角色删除被拒绝：不应有任何关联清理副作用
        verify(userRoleMapper, never()).delete(any(QueryWrapper.class));
        verify(rolePermissionMapper, never()).delete(any(QueryWrapper.class));
        verify(dataScopeMapper, never()).delete(any(QueryWrapper.class));
    }

    @Test
    void assignPermissionsBumpsHolderSessionVersions() {
        when(roleMapper.selectById(3L)).thenReturn(role(3L, "COUNSELOR"));
        when(permissionMapper.selectById(10L)).thenReturn(permission(10L));
        when(rolePermissionMapper.delete(any(QueryWrapper.class))).thenReturn(1);
        // 持有该角色的用户
        when(userRoleMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(
                userRole(5L, 3L)));
        User holder = new User();
        holder.setId(5L);
        holder.setPermissionVersion(2);
        when(userMapper.selectById(5L)).thenReturn(holder);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        service.assignPermissions(3L, List.of(10L));

        verify(rolePermissionMapper).delete(any(QueryWrapper.class));
        verify(rolePermissionMapper).insert(any(RolePermission.class));
        assertEquals(3, holder.getPermissionVersion());
        verify(redisTokenService).storePermissionVersion(5L, 3);
    }

    @Test
    void assignDataScopesRejectsNullRole() {
        when(roleMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.assignDataScopes(99L,
                List.of(new UserDataScopeDTO())));
    }

    private Role role(Long id, String code) {
        Role r = new Role();
        r.setId(id);
        r.setRoleCode(code);
        r.setStatus(1);
        r.setDeleted(0);
        return r;
    }

    private com.example.student.entity.Permission permission(Long id) {
        com.example.student.entity.Permission p = new com.example.student.entity.Permission();
        p.setId(id);
        return p;
    }

    private UserRole userRole(Long userId, Long roleId) {
        UserRole ur = new UserRole();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        return ur;
    }
}