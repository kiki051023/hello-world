package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.student.entity.Student;
import com.example.student.entity.StudentStatusHistory;
import com.example.student.entity.User;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.StudentMapper;
import com.example.student.mapper.StudentStatusHistoryMapper;
import com.example.student.security.DataScopeService;
import com.example.student.security.StudentDataScopeService;
import com.example.student.service.StudentStatusService;
import com.example.student.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StudentStatusServiceImpl implements StudentStatusService {

    private static final Set<String> VALID_TYPES =
            Set.of("STATUS", "TRANSFER_MAJOR", "TRANSFER_CLASS", "SUSPEND", "RESUME", "GRADUATE");

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private StudentStatusHistoryMapper historyMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private StudentDataScopeService studentDataScopeService;

    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public void applyChange(Long studentId, String changeType, String oldValue, String newValue, String reason) {
        if (!VALID_TYPES.contains(changeType)) throw new BusinessException("不支持的异动类型: " + changeType);
        Student student = studentMapper.selectById(studentId);
        if (student == null) throw new BusinessException("学生不存在");
        // 辅导员仅能管理负责班级；学生无权发起异动
        if (!dataScopeService.canAccessStudent(student))
            throw new AccessDeniedException("无权管理该学生学籍");

        Student patch = new Student();
        patch.setId(studentId);
        switch (changeType) {
            case "TRANSFER_MAJOR" -> patch.setMajorId(parseId(newValue, "专业ID"));
            case "TRANSFER_CLASS" -> patch.setClazzId(parseId(newValue, "班级ID"));
            case "SUSPEND", "RESUME", "GRADUATE", "STATUS" -> patch.setStudentStatus(newValue);
            default -> throw new BusinessException("不支持的异动类型");
        }
        studentMapper.updateById(patch);

        StudentStatusHistory h = new StudentStatusHistory();
        h.setStudentId(studentId);
        h.setChangeType(changeType);
        h.setOldValue(oldValue);
        h.setNewValue(newValue);
        h.setReason(reason);
        fillOperator(h);
        historyMapper.insert(h);
    }

    @Override
    public List<StudentStatusHistory> history(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) throw new BusinessException("学生不存在");
        Long resolved = studentDataScopeService.resolveStudentId(studentId);
        return historyMapper.selectList(new LambdaQueryWrapper<StudentStatusHistory>()
                .eq(StudentStatusHistory::getStudentId, resolved)
                .orderByDesc(StudentStatusHistory::getId));
    }

    @Override
    public Map<String, Object> historyPage(long current, long size, Long studentId) {
        Long resolved = studentDataScopeService.resolveStudentId(studentId);
        LambdaQueryWrapper<StudentStatusHistory> w = new LambdaQueryWrapper<StudentStatusHistory>()
                .eq(resolved != null, StudentStatusHistory::getStudentId, resolved)
                .orderByDesc(StudentStatusHistory::getId);
        long total = historyMapper.selectCount(w);
        List<StudentStatusHistory> rows = historyMapper.selectList(w.last("LIMIT " + (current - 1) * size + ", " + size));
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("records", rows);
        return result;
    }

    private Long parseId(String value, String label) {
        try {
            return value == null ? null : Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(label + "格式不正确");
        }
    }

    private void fillOperator(StudentStatusHistory h) {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                User user = userService.getByUsername(auth.getName());
                if (user != null) {
                    h.setOperatorId(user.getId());
                    h.setOperatorName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                }
            }
        } catch (Exception ignored) {
            // 审计操作者尽力而为
        }
    }
}
