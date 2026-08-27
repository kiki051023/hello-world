package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.LeaveRequest;
import com.example.student.entity.Student;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.LeaveRequestMapper;
import com.example.student.mapper.StudentMapper;
import com.example.student.security.DataScopeFilter;
import com.example.student.security.DataScopeService;
import com.example.student.security.StudentDataScopeService;
import com.example.student.service.LeaveRequestService;
import com.example.student.service.NotificationSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LeaveRequestServiceImpl extends ServiceImpl<LeaveRequestMapper, LeaveRequest>
        implements LeaveRequestService {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private StudentDataScopeService studentDataScopeService;

    @Autowired
    private NotificationSender notificationSender;

    @Override
    public Map<String, Object> getPage(long current, long size, Long studentId, String status) {
        DataScopeFilter scope = dataScopeService.currentFilter();
        // 学生强制只看本人；其余角色按数据范围在内存过滤前先收窄（数据量可控，后续可下沉 SQL）
        Long scopedStudentId = studentDataScopeService.resolveStudentId(studentId);
        LambdaQueryWrapper<LeaveRequest> w = new LambdaQueryWrapper<LeaveRequest>()
                .eq(scopedStudentId != null, LeaveRequest::getStudentId, scopedStudentId)
                .eq(status != null && !status.isBlank(), LeaveRequest::getStatus, status)
                .orderByDesc(LeaveRequest::getId);
        long total = count(w);
        List<LeaveRequest> rows = list(w.last("LIMIT " + (current - 1) * size + ", " + size));
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("records", rows);
        return result;
    }

    @Override
    @Transactional
    public void submit(LeaveRequest request) {
        if (request.getStudentId() == null) throw new BusinessException("学生不能为空");
        Student student = studentMapper.selectById(request.getStudentId());
        if (student == null) throw new BusinessException("学生不存在");
        // 学生仅能替本人提交；教职工须有该生数据范围
        DataScopeFilter f = dataScopeService.currentFilter();
        boolean self = student.getUserId() != null && student.getUserId().equals(f.getUserId());
        if (!self && !(f.isUnrestricted() || dataScopeService.canAccessStudent(student)))
            throw new AccessDeniedException("无权替该学生提交请假");
        if (request.getStartTime() == null || request.getEndTime() == null)
            throw new BusinessException("请假时间不完整");
        if (!request.getEndTime().isAfter(request.getStartTime()))
            throw new BusinessException("结束时间必须晚于开始时间");
        request.setId(null);
        request.setStatus("PENDING");
        save(request);
    }

    @Override
    @Transactional
    public void approve(Long id, boolean pass, String comment) {
        LeaveRequest lr = getById(id);
        if (lr == null) throw new BusinessException("请假单不存在");
        Student student = studentMapper.selectById(lr.getStudentId());
        // 审批人必须对该生有数据范围（辅导员=负责班级）
        if (!dataScopeService.canAccessStudent(student))
            throw new AccessDeniedException("无权审批该学生的请假");
        if (!"PENDING".equals(lr.getStatus())) throw new BusinessException("该申请已处理");
        lr.setStatus(pass ? "APPROVED" : "REJECTED");
        lr.setApproveComment(comment);
        lr.setApprovedAt(LocalDateTime.now());
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) lr.setApproverName(auth.getName());
        } catch (Exception ignored) { }
        updateById(lr);
        // P2-D 通知：审批结果通知提交学生
        notificationSender.toStudent(student, "LEAVE",
                pass ? "请假审批通过" : "请假审批驳回",
                "您的请假申请（" + (lr.getStartTime() == null ? "" : lr.getStartTime().toLocalDate())
                        + (lr.getEndTime() == null ? "" : " 至 " + lr.getEndTime().toLocalDate())
                        + "）已" + (pass ? "通过" : "驳回"),
                lr.getId());
    }

    @Override
    @Transactional
    public void returnBack(Long id) {
        LeaveRequest lr = getById(id);
        if (lr == null) throw new BusinessException("请假单不存在");
        if (!"APPROVED".equals(lr.getStatus())) throw new BusinessException("仅已批准的请假可销假");
        lr.setStatus("RETURNED");
        lr.setReturnedAt(LocalDateTime.now());
        updateById(lr);
    }
}
