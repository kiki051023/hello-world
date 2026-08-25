package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.PracticeAssignment;
import com.example.student.entity.PracticeLog;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.PracticeAssignmentMapper;
import com.example.student.mapper.PracticeLogMapper;
import com.example.student.security.DataScopeFilter;
import com.example.student.security.DataScopeService;
import com.example.student.service.PracticeLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PracticeLogServiceImpl extends ServiceImpl<PracticeLogMapper, PracticeLog>
        implements PracticeLogService {

    @Autowired
    @Lazy
    private PracticeAssignmentMapper assignmentMapper;

    @Autowired
    private DataScopeService dataScopeService;

    private boolean canManageAssignment(Long assignmentId) {
        PracticeAssignment a = assignmentMapper.selectById(assignmentId);
        if (a == null) return false;
        DataScopeFilter f = dataScopeService.currentFilter();
        return f.isUnrestricted() || (f.getTeacherId() != null && f.getTeacherId().equals(a.getInstructorTeacherId()));
    }

    @Override
    public Map<String, Object> getPage(long current, long size, Long assignmentId, String status) {
        LambdaQueryWrapper<PracticeLog> w = new LambdaQueryWrapper<PracticeLog>()
                .eq(assignmentId != null, PracticeLog::getAssignmentId, assignmentId)
                .eq(status != null && !status.isBlank(), PracticeLog::getStatus, status)
                .orderByAsc(PracticeLog::getWeekNo);
        List<PracticeLog> rows = new ArrayList<>(list(w));
        // 周记可见性跟随其分配的可见性（指导关系锚点）
        rows.removeIf(l -> !canManageAssignment(l.getAssignmentId()));
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("records", rows);
        return result;
    }

    @Override
    @Transactional
    public void submit(PracticeLog log) {
        if (log.getAssignmentId() == null) throw new BusinessException("实习分配不能为空");
        if (log.getWeekNo() == null || log.getWeekNo() < 1) throw new BusinessException("周次不正确");
        if (!canManageAssignment(log.getAssignmentId()))
            throw new AccessDeniedException("无权为该实习提交周记");
        Long exists = count(new LambdaQueryWrapper<PracticeLog>()
                .eq(PracticeLog::getAssignmentId, log.getAssignmentId())
                .eq(PracticeLog::getWeekNo, log.getWeekNo()));
        if (exists != null && exists > 0) throw new BusinessException("该周周记已存在");
        log.setId(null);
        log.setStatus("SUBMITTED");
        save(log);
    }

    @Override
    @Transactional
    public void comment(Long id, String comment) {
        PracticeLog l = getById(id);
        if (l == null) throw new BusinessException("周记不存在");
        if (!canManageAssignment(l.getAssignmentId()))
            throw new AccessDeniedException("仅指导教师可点评该周记");
        if (!"SUBMITTED".equals(l.getStatus())) throw new BusinessException("仅已提交周记可点评");
        l.setStatus("COMMENTED");
        l.setTeacherComment(comment);
        l.setCommentedAt(LocalDateTime.now());
        updateById(l);
    }
}
