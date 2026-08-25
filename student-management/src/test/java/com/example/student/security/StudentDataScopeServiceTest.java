package com.example.student.security;

import com.example.student.entity.Student;
import com.example.student.entity.User;
import com.example.student.mapper.StudentMapper;
import com.example.student.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentDataScopeServiceTest {

    private final UserService userService = mock(UserService.class);
    private final StudentMapper studentMapper = mock(StudentMapper.class);
    private final StudentDataScopeService service = new StudentDataScopeService(userService, studentMapper);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void studentScopeAlwaysUsesTheAuthenticatedStudent() {
        User user = new User();
        user.setId(7L);
        user.setUsername("student1");
        Student student = new Student();
        student.setId(11L);
        student.setUserId(7L);
        when(userService.getByUsername("student1")).thenReturn(user);
        when(studentMapper.selectOne(any())).thenReturn(student);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student1", null, java.util.List.of(() -> "ROLE_STUDENT")));

        assertThat(service.resolveStudentId(99L)).isEqualTo(11L);
    }

    @Test
    void studentScopeRejectsAnUnlinkedAccount() {
        User user = new User();
        user.setId(7L);
        user.setUsername("student1");
        when(userService.getByUsername("student1")).thenReturn(user);
        when(studentMapper.selectOne(any())).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student1", null, java.util.List.of(() -> "ROLE_STUDENT")));

        assertThatThrownBy(() -> service.resolveStudentId(null))
                .isInstanceOf(AccessDeniedException.class);
    }
}
