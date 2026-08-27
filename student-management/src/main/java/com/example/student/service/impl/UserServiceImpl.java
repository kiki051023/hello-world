package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.User;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.UserMapper;
import com.example.student.security.RedisTokenService;
import com.example.student.service.UserService;
import com.example.student.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RedisTokenService redisTokenService;

    @Autowired
    private com.example.student.security.LoginLockoutService loginLockoutService;

    /** 假 BCrypt 比对用固定哈希（密码"123456"的哈希），使"用户不存在"与"密码错误"耗时一致，防用户名枚举 */
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @Override
    public Map<String, Object> login(String username, String password) {
        if (loginLockoutService.isLocked(username)) {
            throw new BusinessException("失败次数过多，账号已临时锁定，请15分钟后重试");
        }
        User user = getByUsername(username);
        if (user == null) {
            loginLockoutService.recordFailure(username);
            // 耗时统一：执行一次与真实校验相同成本的假 BCrypt 比对，防止通过响应时间区分用户名是否存在
            passwordEncoder.matches(password, DUMMY_PASSWORD_HASH);
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            loginLockoutService.recordFailure(username);
            throw new BusinessException("用户名或密码错误");
        }
        loginLockoutService.resetFailures(username);
        Integer permissionVersion = user.getPermissionVersion() == null ? 1 : user.getPermissionVersion();
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), user.getRole(), permissionVersion);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername(), user.getRole(), permissionVersion);
        redisTokenService.storePermissionVersion(user.getId(), permissionVersion);
        // 记录刷新令牌，便于登出吊销与刷新校验
        redisTokenService.storeRefreshToken(user.getId(), jwtUtils.getJtiFromToken(refreshToken),
                jwtUtils.getRefreshExpireSeconds());

        Map<String, Object> result = new HashMap<>();
        result.put("token", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("role", user.getRole());
        // P1：首次登录/管理员重置密码后需强制改密
        result.put("mustChangePassword",
                user.getMustChangePassword() != null && user.getMustChangePassword() == 1);
        return result;
    }

    @Override
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    @Transactional
    public Map<String, Object> changePassword(Long userId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 8
                || !newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")) {
            throw new BusinessException("新密码必须包含大写字母、小写字母和数字，且长度至少8位");
        }
        if (newPassword.equals(oldPassword)) {
            throw new BusinessException("新密码不能与旧密码相同");
        }
        User user = getById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }
        return applyNewPassword(user, newPassword);
    }

    @Override
    @Transactional
    public Map<String, Object> forceChangePassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 8
                || !newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")) {
            throw new BusinessException("新密码必须包含大写字母、小写字母和数字，且长度至少8位");
        }
        User user = getById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        // 仅允许"待强制改密"状态调用；否则必须走带旧密码的 changePassword
        if (user.getMustChangePassword() == null || user.getMustChangePassword() != 1) {
            throw new BusinessException("当前账号无需强制改密，请使用常规修改密码");
        }
        return applyNewPassword(user, newPassword);
    }

    /** 公共改密落地：更新密码 + 清标记 + 版本+1 + 换发令牌 */
    @Transactional
    protected Map<String, Object> applyNewPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(0);
        user.setLastPasswordChange(LocalDateTime.now());
        Integer newVersion = (user.getPermissionVersion() == null ? 1 : user.getPermissionVersion()) + 1;
        user.setPermissionVersion(newVersion);
        updateById(user);

        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), user.getRole(), newVersion);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername(), user.getRole(), newVersion);
        redisTokenService.storePermissionVersion(user.getId(), newVersion);
        redisTokenService.storeRefreshToken(user.getId(), jwtUtils.getJtiFromToken(refreshToken),
                jwtUtils.getRefreshExpireSeconds());

        Map<String, Object> result = new HashMap<>();
        result.put("token", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("role", user.getRole());
        result.put("mustChangePassword", false);
        return result;
    }
}