package com.example.student.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.example.student.service.impl.UserManageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用户管理核心边界：用户名唯一、密码强度、内置 ADMIN 防自禁、会话失效。
 */
class UserManageServiceTest {

    private UserManageServiceImpl service;
    private UserMapper userMapper;
    private RoleMapper roleMapper;
    private UserRoleMapper userRoleMapper;
    private DataScopeMapper dataScopeMapper;
    private RedisTokenService redisTokenService;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        service = new UserManageServiceImpl();
        userMapper = mock(UserMapper.class);
        roleMapper = mock(RoleMapper.class);
        userRoleMapper = mock(UserRoleMapper.class);
        dataScopeMapper = mock(DataScopeMapper.class);
        redisTokenService = mock(RedisTokenService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userService = mock(UserService.class);

        ReflectionTestUtils.setField(service, "baseMapper", userMapper);
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(service, "redisTokenService", redisTokenService);
        ReflectionTestUtils.setField(service, "roleMapper", roleMapper);
        ReflectionTestUtils.setField(service, "userRoleMapper", userRoleMapper);
        ReflectionTestUtils.setField(service, "dataScopeMapper", dataScopeMapper);
        ReflectionTestUtils.setField(service, "userService", userService);
    }

    private User sampleUser(Long id, String role) {
        User u = new User();
        u.setId(id);
        u.setUsername("user" + id);
        u.setRole(role);
        u.setStatus(1);
        u.setPermissionVersion(1);
        return u;
    }

    @Test
    void createUserRejectsDuplicateUsername() {
        when(userService.getByUsername("dup")).thenReturn(sampleUser(9L, "STUDENT"));
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("dup");
        dto.setPassword("Abc@123456");
        dto.setRealName("重复用户");
        assertThrows(BusinessException.class, () -> service.createUser(dto));
        verify(userMapper, never()).insert(any());
    }

    @Test
    void createUserEncodesPasswordAndBindsRoles() {
        when(userService.getByUsername("newbie")).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userMapper.insert(any(User.class))).thenReturn(1);
        when(roleMapper.selectById(5L)).thenReturn(role(5L, "TEACHER"));

        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("newbie");
        dto.setPassword("Abc@123456");
        dto.setRealName("新用户");
        dto.setRole("TEACHER");
        dto.setRoleIds(List.of(5L));
        service.createUser(dto);

        verify(userMapper).insert(any(User.class));
        verify(userRoleMapper).insert(any(UserRole.class));
    }

    @Test
    void createUserRejectsWeakPassword() {
        when(userService.getByUsername("weak")).thenReturn(null);
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("weak");
        dto.setPassword("123456"); // 纯数字，不满足大写+小写+数字
        dto.setRealName("弱密码");
        assertThrows(Exception.class, () -> service.createUser(dto));
    }

    @Test
    void resetPasswordValidatesStrength() {
        when(userMapper.selectById(1L)).thenReturn(sampleUser(1L, "TEACHER"));
        assertThrows(BusinessException.class, () -> service.resetPassword(1L, "weak"));
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void resetPasswordRevokesRefreshAndBumpsVersion() {
        User u = sampleUser(1L, "TEACHER");
        when(userMapper.selectById(1L)).thenReturn(u);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$new");
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        service.resetPassword(1L, "NewPass123");

        assertEquals(2, u.getPermissionVersion());
        verify(redisTokenService).storePermissionVersion(1L, 2);
        verify(redisTokenService).removeRefreshToken(1L);
    }

    @Test
    void changeStatusRejectsDisablingSelf() {
        User admin = sampleUser(1L, "ADMIN");
        when(userMapper.selectById(1L)).thenReturn(admin);
        when(userService.getByUsername("user1")).thenReturn(admin);
        // 模拟当前登录用户即 admin（JWT principal 名称为 username）
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "user1", null, java.util.List.of()));
        try {
            assertThrows(BusinessException.class, () -> service.changeStatus(1L, 0));
            verify(userMapper, never()).updateById(any());
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    void assignRolesRebindsAndBumpsVersion() {
        User u = sampleUser(2L, "STUDENT");
        when(userMapper.selectById(2L)).thenReturn(u);
        when(roleMapper.selectById(9L)).thenReturn(role(9L, "COUNSELOR"));
        when(userRoleMapper.delete(any(QueryWrapper.class))).thenReturn(1);

        service.assignRoles(2L, List.of(9L));

        assertEquals(2, u.getPermissionVersion());
        verify(userRoleMapper).delete(any(QueryWrapper.class));
        verify(userRoleMapper).insert(any(UserRole.class));
    }

    private Role role(Long id, String code) {
        Role r = new Role();
        r.setId(id);
        r.setRoleCode(code);
        r.setStatus(1);
        r.setDeleted(0);
        return r;
    }
}