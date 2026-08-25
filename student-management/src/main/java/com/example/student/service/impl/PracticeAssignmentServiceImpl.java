package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.PracticeAssignment;
import com.example.student.entity.Student;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.PracticeAssignmentMapper;
import com.example.student.mapper.StudentMapper;
import com.example.student.security.DataScopeFilter;
import com.example.student.security.DataScopeService;
import com.example.student.security.StudentDataScopeService;
import com.example.student.service.PracticeAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PracticeAssignmentServiceImpl extends ServiceImpl<PracticeAssignmentMapper, PracticeAssignment>
        implements PracticeAssignmentService {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private StudentDataScopeService studentDataScopeService;

    /** 指导教师仅看本人负责的分配；学生仅看本人。 */
    private boolean visible(PracticeAssignment a, DataScopeFilter f) {
        if (f.isUnrestricted()) return true;
        if (f.getTeacherId() != null && f.getTeacherId().equals(a.getInstructorTeacherId())) return true;
        Student s = studentMapper.selectById(a.getStudentId());
        boolean self = s != null && s.getUserId() != null && s.getUserId().equals(f.getUserId());
        return self || dataScopeService.canAccessStudent(s);
    }

    @Override
    public Map<String, Object> getPage(long current, long size, Long planId, Long studentId) {
        DataScopeFilter f = dataScopeService.currentFilter();
        Long scopedStudentId = studentDataScopeService.resolveStudentId(studentId);
        LambdaQueryWrapper<PracticeAssignment> w = new LambdaQueryWrapper<PracticeAssignment>()
                .eq(planId != null, PracticeAssignment::getPlanId, planId)
                .eq(scopedStudentId != null, PracticeAssignment::getStudentId, scopedStudentId)
                .orderByDesc(PracticeAssignment::getId);
        List<PracticeAssignment> rows = new ArrayList<>(
                list(w.last("LIMIT " + (current - 1) * size + ", " + size)));
        rows.removeIf(r -> !visible(r, f));
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("records", rows);
        return result;
    }

    @Override
    public PracticeAssignment getByIdChecked(Long id) {
        PracticeAssignment a = getById(id);
        if (a == null) throw new BusinessException("实习分配不存在");
        if (!visible(a, dataScopeService.currentFilter()))
            throw new AccessDeniedException("无权查看该实习记录");
        return a;
    }

    @Override
    @Transactional
    public void assign(PracticeAssignment assignment) {
        if (assignment.getStudentId() == null || assignment.getCompanyId() == null
                || assignment.getInstructorTeacherId() == null)
            throw new BusinessException("学生、企业与指导教师不能为空");
        // 指导教师只能被授权人指派；指派后教师自动获得该生实习的数据范围（经 instructorTeacherId 锚点）
        DataScopeFilter f = dataScopeService.currentFilter();
        if (!f.isUnrestricted() && !f.getTeacherId().equals(assignment.getInstructorTeacherId()))
            throw new AccessDeniedException("无权建立该校内指导关系");
        Long exists = count(new LambdaQueryWrapper<PracticeAssignment>()
                .eq(PracticeAssignment::getPlanId, assignment.getPlanId())
                .eq(PracticeAssignment::getStudentId, assignment.getStudentId()));
        if (exists != null && exists > 0)
            throw new BusinessException("该生已在此计划中");
        assignment.setId(null);
        if (assignment.getStatus() == null) assignment.setStatus("ACTIVE");
        save(assignment);
    }

    @Override
    @Transactional
    public void finish(Long id, BigDecimal score) {
        PracticeAssignment a = getByIdChecked(id);
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0)
            throw new BusinessException("实习成绩须在 0-100 之间");
        a.setStatus("FINISHED");
        a.setPracticeScore(score);
        updateById(a);
    }
}
