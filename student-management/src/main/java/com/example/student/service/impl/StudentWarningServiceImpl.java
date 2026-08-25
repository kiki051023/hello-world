package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.StudentWarning;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.StudentMapper;
import com.example.student.mapper.StudentWarningMapper;
import com.example.student.security.DataScopeFilter;
import com.example.student.security.DataScopeService;
import com.example.student.service.StudentWarningService;
import com.example.student.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 预警：规则引擎按 (student, type, ruleKey) 幂等生成 ACTIVE 预警；处理/驳回写操作者。 */
@Service
public class StudentWarningServiceImpl extends ServiceImpl<StudentWarningMapper, StudentWarning>
        implements StudentWarningService {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private UserService userService;

    @Override
    public Map<String, Object> getPage(long current, long size, Long studentId, String status) {
        DataScopeFilter filter = dataScopeService.currentFilter();
        LambdaQueryWrapper<StudentWarning> w = new LambdaQueryWrapper<StudentWarning>()
                .eq(studentId != null, StudentWarning::getStudentId, studentId)
                .eq(status != null && !status.isBlank(), StudentWarning::getStatus, status)
                .orderByDesc(StudentWarning::getId);
        List<StudentWarning> rows = new java.util.ArrayList<>(
                list(w.last("LIMIT " + (current - 1) * size + ", " + size)));
        rows.removeIf(warn -> {
            var s = studentMapper.selectById(warn.getStudentId());
            return !(filter.isUnrestricted() || dataScopeService.canAccessStudent(s));
        });
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("records", rows);
        return result;
    }

    @Override
    @Transactional
    public int generateByRule(String warningType, String ruleKey, int threshold, Map<Long, Integer> studentValueMap) {
        int created = 0;
        for (Map.Entry<Long, Integer> e : studentValueMap.entrySet()) {
            if (e.getValue() == null || e.getValue() < threshold) continue;
            Long exists = count(new LambdaQueryWrapper<StudentWarning>()
                    .eq(StudentWarning::getStudentId, e.getKey())
                    .eq(StudentWarning::getWarningType, warningType)
                    .eq(StudentWarning::getRuleKey, ruleKey)
                    .eq(StudentWarning::getStatus, "ACTIVE"));
            if (exists != null && exists > 0) continue; // 幂等
            StudentWarning warn = new StudentWarning();
            warn.setStudentId(e.getKey());
            warn.setWarningType(warningType);
            warn.setRuleKey(ruleKey);
            warn.setRuleThreshold(threshold);
            warn.setCurrentValue(e.getValue());
            warn.setStatus("ACTIVE");
            save(warn);
            created++;
        }
        return created;
    }

    @Override
    @Transactional
    public void handle(Long id, boolean dismiss, String note) {
        StudentWarning warn = getById(id);
        if (warn == null) throw new BusinessException("预警不存在");
        var s = studentMapper.selectById(warn.getStudentId());
        if (!dataScopeService.currentFilter().isUnrestricted() && !dataScopeService.canAccessStudent(s))
            throw new AccessDeniedException("无权处理该预警");
        if (!"ACTIVE".equals(warn.getStatus())) throw new BusinessException("该预警已处理");
        warn.setStatus(dismiss ? "DISMISSED" : "HANDLED");
        warn.setHandleNote(note);
        warn.setHandledAt(LocalDateTime.now());
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                var user = userService.getByUsername(auth.getName());
                if (user != null) warn.setHandledBy(user.getId());
            }
        } catch (Exception ignored) {
            // 操作者信息尽力而为
        }
        updateById(warn);
    }
}
