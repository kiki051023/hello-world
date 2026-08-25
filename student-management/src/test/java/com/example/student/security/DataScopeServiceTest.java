package com.example.student.security;

import com.example.student.entity.DataScope;
import com.example.student.entity.User;
import com.example.student.mapper.RoleMapper;
import com.example.student.mapper.TeacherMapper;
import com.example.student.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class DataScopeServiceTest {
    @Test
    void departmentLeaderGetsDepartmentScope() {
        UserService users = Mockito.mock(UserService.class);
        RoleMapper roles = Mockito.mock(RoleMapper.class);
        TeacherMapper teachers = Mockito.mock(TeacherMapper.class);
        User user = new User();
        user.setId(7L);
        user.setDepartmentId(12L);
        when(users.getByUsername("leader")).thenReturn(user);
        when(roles.selectRoleCodesByUserId(7L)).thenReturn(List.of("DEPARTMENT_LEADER"));
        when(roles.selectDataScopesByUserId(7L)).thenReturn(List.of(new DataScope()));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("leader", "n/a"));

        DataScopeService service = new DataScopeService(users, roles, teachers);
        DataScopeFilter filter = service.currentFilter();

        assertFalse(filter.isUnrestricted());
        assertEquals(7L, filter.getUserId());
        assertEquals(12L, filter.getDepartmentId());
    }

    @Test
    void administratorGetsUnrestrictedScope() {
        UserService users = Mockito.mock(UserService.class);
        RoleMapper roles = Mockito.mock(RoleMapper.class);
        TeacherMapper teachers = Mockito.mock(TeacherMapper.class);
        User user = new User();
        user.setId(1L);
        when(users.getByUsername("admin")).thenReturn(user);
        when(roles.selectRoleCodesByUserId(1L)).thenReturn(List.of("ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "n/a"));

        DataScopeFilter filter = new DataScopeService(users, roles, teachers).currentFilter();

        assertTrue(filter.isUnrestricted());
    }
}
