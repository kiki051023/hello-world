package com.example.student.security;

import com.example.student.entity.User;
import com.example.student.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 数据权限分配守卫：仅 ADMIN 可分配数据权限（用户级/角色级）。
 *
 * 为什么需要独立守卫：@PreAuthorize 只校验角色，而数据权限的分配属于
 * "授信操作"——非 ADMIN 一旦获得该能力，可借机扩大自身数据范围绕过行级过滤。
 * 此守卫在服务层/控制器调用前强制校验当前用户角色 == ADMIN。
 */
@Component
public class RoleManageDataScopeGuard {

    private final UserService userService;

    public RoleManageDataScopeGuard(UserService userService) {
        this.userService = userService;
    }

    public void assertAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("未登录");
        }
        User user = userService.getByUsername(authentication.getName());
        if (user == null) {
            throw new AccessDeniedException("用户不存在");
        }
        boolean admin = "ADMIN".equalsIgnoreCase(user.getRole())
                || authentication.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!admin) {
            throw new AccessDeniedException("仅系统管理员可以分配数据权限");
        }
    }
}