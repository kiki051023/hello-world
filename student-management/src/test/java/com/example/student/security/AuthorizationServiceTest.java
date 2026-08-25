package com.example.student.security;

import com.example.student.entity.Permission;
import com.example.student.entity.User;
import com.example.student.mapper.RoleMapper;
import com.example.student.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class AuthorizationServiceTest {
    private final UserService users = mock(UserService.class);
    private final RoleMapper roles = mock(RoleMapper.class);
    private final AuthorizationService service = new AuthorizationService(users, roles);

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void resolvesPermissionFromAuthenticatedUsersRoleAssignments() {
        User user = new User(); user.setId(3L); user.setUsername("admin");
        Permission permission = new Permission(); permission.setPermissionCode("score:read");
        when(users.getByUsername("admin")).thenReturn(user);
        when(roles.selectPermissionsByUserId(3L)).thenReturn(java.util.List.of(permission));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, java.util.List.of()));

        assertThat(service.hasPermission("score:read")).isTrue();
        verify(roles).selectPermissionsByUserId(anyLong());
    }
}
