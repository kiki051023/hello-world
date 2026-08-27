package com.example.student.service;

import com.example.student.entity.User;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.UserMapper;
import com.example.student.security.LoginLockoutService;
import com.example.student.security.RedisTokenService;
import com.example.student.service.impl.UserServiceImpl;
import com.example.student.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * P1 密码策略：
 * - changePassword：旧密码校验、强度、与旧密码不同、清强制改密、权限版本+1、吊销刷新
 * - login：用户不存在也执行假 BCrypt（耗时统一）、统一文案、返回 mustChangePassword
 */
class UserPasswordPolicyTest {

    private UserServiceImpl service;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private RedisTokenService redisTokenService;
    private LoginLockoutService loginLockoutService;
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl();
        userMapper = mock(UserMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        redisTokenService = mock(RedisTokenService.class);
        loginLockoutService = mock(LoginLockoutService.class);
        jwtUtils = mock(JwtUtils.class);

        ReflectionTestUtils.setField(service, "baseMapper", userMapper);
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(service, "redisTokenService", redisTokenService);
        ReflectionTestUtils.setField(service, "loginLockoutService", loginLockoutService);
        ReflectionTestUtils.setField(service, "jwtUtils", jwtUtils);
    }

    private User sampleUser(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("u" + id);
        u.setPassword("$2a$10$oldHash");
        u.setStatus(1);
        u.setPermissionVersion(3);
        u.setMustChangePassword(1);
        return u;
    }

    @Test
    void changePasswordRejectsWeakNewPassword() {
        when(userMapper.selectById(1L)).thenReturn(sampleUser(1L));
        assertThrows(BusinessException.class, () -> service.changePassword(1L, "OldPass1", "weak"));
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void changePasswordRejectsSameAsOld() {
        when(userMapper.selectById(1L)).thenReturn(sampleUser(1L));
        assertThrows(BusinessException.class, () -> service.changePassword(1L, "Same1234", "Same1234"));
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void changePasswordRejectsWrongOldPassword() {
        when(userMapper.selectById(1L)).thenReturn(sampleUser(1L));
        when(passwordEncoder.matches(eq("WrongOld"), anyString())).thenReturn(false);
        assertThrows(BusinessException.class, () -> service.changePassword(1L, "WrongOld", "NewPass123"));
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void changePasswordClearsMustChangeAndBumpsVersion() {
        User u = sampleUser(1L);
        when(userMapper.selectById(1L)).thenReturn(u);
        when(passwordEncoder.matches(eq("OldPass1"), anyString())).thenReturn(true);
        when(passwordEncoder.encode("NewPass123")).thenReturn("$2a$10$newHash");
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(jwtUtils.generateAccessToken(eq(1L), any(), any(), eq(4)))
                .thenReturn("access.new");
        when(jwtUtils.generateRefreshToken(eq(1L), any(), any(), eq(4)))
                .thenReturn("refresh.new");
        when(jwtUtils.getJtiFromToken("refresh.new")).thenReturn("jti-new");
        when(jwtUtils.getRefreshExpireSeconds()).thenReturn(604800L);

        var result = service.changePassword(1L, "OldPass1", "NewPass123");

        assertEquals(0, u.getMustChangePassword());
        assertEquals(4, u.getPermissionVersion());
        assertEquals("$2a$10$newHash", u.getPassword());
        assertNotNull(u.getLastPasswordChange());
        // 换发新令牌：persist 新 refresh（不再 remove）
        assertEquals("access.new", result.get("token"));
        assertEquals("refresh.new", result.get("refreshToken"));
        assertEquals(false, result.get("mustChangePassword"));
        verify(redisTokenService).storePermissionVersion(1L, 4);
        verify(redisTokenService).storeRefreshToken(1L, "jti-new", 604800L);
        verify(redisTokenService, never()).removeRefreshToken(1L);
    }

    @Test
    void loginWithUnknownUserRunsDummyBcryptAndUnifiedMessage() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        when(loginLockoutService.isLocked("ghost")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.login("ghost", "whatever1"));
        // 统一文案：不暴露用户名是否存在
        assertEquals("用户名或密码错误", ex.getMessage());
        // 耗时统一：执行了一次假 BCrypt 比对
        verify(passwordEncoder).matches(eq("whatever1"), anyString());
        verify(loginLockoutService).recordFailure("ghost");
    }

    @Test
    void loginReturnsMustChangePasswordFlag() {
        User u = sampleUser(7L);
        u.setMustChangePassword(1);
        when(userMapper.selectOne(any())).thenReturn(u);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(loginLockoutService.isLocked("u7")).thenReturn(false);
        when(loginLockoutService.getMaxAttempts()).thenReturn(5);

        var result = service.login("u7", "Correct1");
        assertTrue((Boolean) result.get("mustChangePassword"));
    }

    @Test
    void loginPasswordMismatchUnifiedMessage() {
        User u = sampleUser(7L);
        when(userMapper.selectOne(any())).thenReturn(u);
        when(passwordEncoder.matches(eq("BadPass1"), anyString())).thenReturn(false);
        when(loginLockoutService.isLocked("u7")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.login("u7", "BadPass1"));
        assertEquals("用户名或密码错误", ex.getMessage());
        verify(loginLockoutService).recordFailure("u7");
    }

    // ---------- 强制改密 ----------

    @Test
    void forceChangePasswordRejectsWhenNotRequired() {
        User u = sampleUser(1L);
        u.setMustChangePassword(0); // 不需强制改密
        when(userMapper.selectById(1L)).thenReturn(u);

        assertThrows(BusinessException.class, () -> service.forceChangePassword(1L, "NewPass123"));
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void forceChangePasswordAppliesAndIssuesNewTokens() {
        User u = sampleUser(1L);
        u.setMustChangePassword(1);
        when(userMapper.selectById(1L)).thenReturn(u);
        when(passwordEncoder.encode("NewPass123")).thenReturn("$2a$10$forceHash");
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(jwtUtils.generateAccessToken(eq(1L), any(), any(), eq(4))).thenReturn("access.force");
        when(jwtUtils.generateRefreshToken(eq(1L), any(), any(), eq(4))).thenReturn("refresh.force");
        when(jwtUtils.getJtiFromToken("refresh.force")).thenReturn("jti-force");
        when(jwtUtils.getRefreshExpireSeconds()).thenReturn(604800L);

        var result = service.forceChangePassword(1L, "NewPass123");

        assertEquals(0, u.getMustChangePassword());
        assertEquals(4, u.getPermissionVersion());
        assertEquals("$2a$10$forceHash", u.getPassword());
        assertEquals("access.force", result.get("token"));
        assertEquals("refresh.force", result.get("refreshToken"));
        verify(redisTokenService).storePermissionVersion(1L, 4);
        verify(redisTokenService).storeRefreshToken(1L, "jti-force", 604800L);
    }

    @Test
    void forceChangePasswordRejectsWeak() {
        User u = sampleUser(1L);
        u.setMustChangePassword(1);
        when(userMapper.selectById(1L)).thenReturn(u);
        assertThrows(BusinessException.class, () -> service.forceChangePassword(1L, "weak"));
        verify(userMapper, never()).updateById(any());
    }
}