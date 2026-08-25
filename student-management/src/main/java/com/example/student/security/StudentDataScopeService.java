package com.example.student.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.student.entity.Student;
import com.example.student.entity.User;
import com.example.student.mapper.StudentMapper;
import com.example.student.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Resolves the student data scope from the server-side authenticated identity. */
@Service
public class StudentDataScopeService {

    private final UserService userService;
    private final StudentMapper studentMapper;

    public StudentDataScopeService(UserService userService, StudentMapper studentMapper) {
        this.userService = userService;
        this.studentMapper = studentMapper;
    }

    /**
     * Returns the requested ID for non-students, and the linked student ID for students.
     * A student cannot widen the scope by supplying another ID.
     */
    public Long resolveStudentId(Long requestedStudentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_STUDENT".equals(authority.getAuthority()))) {
            return requestedStudentId;
        }

        User user = userService.getByUsername(authentication.getName());
        if (user == null) {
            throw new AccessDeniedException("当前账号不存在");
        }
        Student student = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getUserId, user.getId()));
        if (student == null || student.getId() == null) {
            throw new AccessDeniedException("当前账号未关联学生档案");
        }
        return student.getId();
    }
}
