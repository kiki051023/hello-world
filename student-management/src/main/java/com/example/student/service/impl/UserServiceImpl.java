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

    @Override
    public Map<String, Object> login(String username, String password) {
        if (loginLockoutService.isLocked(username)) {
            throw new BusinessException("失败次数过多，账号已临时锁定，请15分钟后重试");
        }
        User user = getByUsername(username);
        if (user == null) {
            loginLockoutService.recordFailure(username);
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            loginLockoutService.recordFailure(username);
            throw new BusinessException("密码错误");
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
        return result;
    }

    @Override
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return baseMapper.selectOne(wrapper);
    }
}
