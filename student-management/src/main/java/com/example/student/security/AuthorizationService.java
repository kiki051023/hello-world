package com.example.student.security;

import com.example.student.entity.DataScope;
import com.example.student.entity.Permission;
import com.example.student.entity.User;
import com.example.student.mapper.RoleMapper;
import com.example.student.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthorizationService {
    private final UserService userService;
    private final RoleMapper roleMapper;

    public AuthorizationService(UserService userService, RoleMapper roleMapper) {
        this.userService = userService;
        this.roleMapper = roleMapper;
    }

    public User currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("未登录");
        }
        User user = userService.getByUsername(authentication.getName());
        if (user == null) throw new AccessDeniedException("用户不存在");
        return user;
    }

    public boolean hasPermission(String permissionCode) {
        return permissions().stream().anyMatch(p -> permissionCode.equals(p.getPermissionCode()));
    }

    public List<Permission> permissions() {
        return roleMapper.selectPermissionsByUserId(currentUser().getId());
    }

    public List<DataScope> dataScopes() {
        return roleMapper.selectDataScopesByUserId(currentUser().getId());
    }
}
