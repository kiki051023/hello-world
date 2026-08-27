package com.example.student.security;

import com.example.student.entity.User;
import com.example.student.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 数据权限分配守卫：仅 ADMIN 可分配数据权限，其余角色一律拒绝。
 */
class RoleManageDataScopeGuardTest {

    private RoleManageDataScopeGuard guard;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        guard = new RoleManageDataScopeGuard(userService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setRole(role);
        when(userService.getByUsername(username)).thenReturn(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test
    void adminCanAssignDataScope() {
        loginAs("admin", "ADMIN");
        assertDoesNotThrow(() -> guard.assertAdmin());
    }

    @Test
    void teacherCannotAssignDataScope() {
        loginAs("teacher1", "TEACHER");
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> guard.assertAdmin());
    }

    @Test
    void studentCannotAssignDataScope() {
        loginAs("student1", "STUDENT");
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> guard.assertAdmin());
    }

    @Test
    void anonymousCannotAssignDataScope() {
        SecurityContextHolder.clearContext();
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> guard.assertAdmin());
    }
}