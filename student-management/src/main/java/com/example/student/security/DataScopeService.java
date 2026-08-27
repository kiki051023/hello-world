package com.example.student.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.student.entity.DataScope;
import com.example.student.entity.Teacher;
import com.example.student.entity.Student;
import com.example.student.entity.Clazz;
import com.example.student.entity.Course;
import com.example.student.entity.Teacher;
import com.example.student.entity.User;
import com.example.student.mapper.RoleMapper;
import com.example.student.mapper.TeacherMapper;
import com.example.student.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Resolves one authenticated user's effective scope for all business queries. */
@Service
public class DataScopeService {
    private final UserService userService;
    private final RoleMapper roleMapper;
    private final TeacherMapper teacherMapper;

    public DataScopeService(UserService userService, RoleMapper roleMapper, TeacherMapper teacherMapper) {
        this.userService = userService;
        this.roleMapper = roleMapper;
        this.teacherMapper = teacherMapper;
    }

    public DataScopeFilter currentFilter() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) throw new AccessDeniedException("未登录");
        User user = userService.getByUsername(authentication.getName());
        if (user == null) throw new AccessDeniedException("用户不存在");

        Set<String> roles = new HashSet<>(roleMapper.selectRoleCodesByUserId(user.getId()));
        DataScopeFilter filter = new DataScopeFilter();
        filter.setUserId(user.getId());
        filter.setSchoolId(user.getSchoolId());
        filter.setDepartmentId(user.getDepartmentId());
        filter.setUnrestricted(roles.stream().anyMatch(Set.of("ADMIN", "EDUCATION_ADMIN", "SCHOOL_LEADER")::contains));
        filter.setSelfOnly(roles.contains("STUDENT") && !filter.isUnrestricted());
        // 学生视角强制隐藏作废成绩（VOID 仅教师/管理可见，用于追溯）
        filter.setHideVoid(roles.contains("STUDENT") && !filter.isUnrestricted());

        if (roles.contains("TEACHER") || roles.contains("PRACTICE_TEACHER")) {
            Teacher teacher = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, user.getId()));
            if (teacher != null) {
                filter.setTeacherId(teacher.getId());
                if (filter.getDepartmentId() == null) filter.setDepartmentId(teacher.getDepartmentId());
            }
        }
        List<DataScope> scopes = roleMapper.selectDataScopesByUserId(user.getId());
        if (roles.contains("COUNSELOR")) filter.setClassIds(scopeIds(scopes, "CLAZZ"));
        filter.getClassIds().addAll(scopeIds(scopes, "CLASS"));
        filter.setCourseIds(scopeIds(scopes, "COURSE"));
        return filter;
    }

    private List<Long> scopeIds(List<DataScope> scopes, String type) {
        return scopes.stream().filter(s -> type.equalsIgnoreCase(s.getScopeType()) && s.getScopeValue() != null)
                .map(DataScope::getScopeValue).distinct().toList();
    }

    public boolean canAccessStudent(Student student) {
        DataScopeFilter f = currentFilter();
        if (f.isUnrestricted()) return true;
        if (f.isSelfOnly()) return student.getUserId() != null && student.getUserId().equals(f.getUserId());
        return (f.getDepartmentId() != null && f.getDepartmentId().equals(student.getDepartmentId()))
                || (f.hasClassIds() && f.getClassIds().contains(student.getClazzId()));
    }

    public boolean canAccessClazz(Clazz clazz) {
        DataScopeFilter f = currentFilter();
        return f.isUnrestricted() || (f.getDepartmentId() != null && f.getDepartmentId().equals(clazz.getDepartmentId()))
                || (f.hasClassIds() && f.getClassIds().contains(clazz.getId()))
                || (f.getTeacherId() != null && f.getTeacherId().equals(clazz.getHeadTeacherId()));
    }

    public boolean canAccessCourse(Course course) {
        if (course == null) return false;
        DataScopeFilter f = currentFilter();
        return f.isUnrestricted() || (f.getTeacherId() != null && f.getTeacherId().equals(course.getTeacherId()))
                || (f.hasCourseIds() && f.getCourseIds().contains(course.getId()));
    }

    public boolean canAccessScore(Student student, Course course) {
        return canAccessStudent(student) || canAccessCourse(course);
    }

    public boolean canAccessTeacher(Teacher teacher) {
        DataScopeFilter f = currentFilter();
        return f.isUnrestricted() || (f.getDepartmentId() != null && f.getDepartmentId().equals(teacher.getDepartmentId()))
                || (f.getTeacherId() != null && f.getTeacherId().equals(teacher.getId()));
    }

    public boolean canAssignTeacher(Long teacherId) {
        DataScopeFilter f = currentFilter();
        if (f.isUnrestricted()) return true;
        if (teacherId == null) return false;
        Teacher teacher = teacherMapper.selectById(teacherId);
        return teacher != null && canAccessTeacher(teacher);
    }
}
