package com.example.student.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.User;

import java.util.Map;

public interface UserService extends IService<User> {

    Map<String, Object> login(String username, String password);

    User getByUsername(String username);

    /**
     * 修改当前用户密码（校验旧密码 + 强度 + 与旧密码不同）。
     * 成功后：清强制改密标记、更新 last_password_change、permissionVersion+1 使旧会话失效，
     * 并换发新的 access/refresh token（否则旧令牌因版本不一致立即失效，前端无法无缝续用）。
     */
    Map<String, Object> changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 强制改密（首次登录/管理员重置密码后）：不校验旧密码（用户不知旧密码），
     * 但要求 mustChangePassword=1（否则拒绝）。成功后逻辑同 changePassword。
     */
    Map<String, Object> forceChangePassword(Long userId, String newPassword);
}