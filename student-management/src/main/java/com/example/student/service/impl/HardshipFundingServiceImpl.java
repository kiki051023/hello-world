package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.HardshipFunding;
import com.example.student.entity.Student;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.HardshipFundingMapper;
import com.example.student.mapper.StudentMapper;
import com.example.student.security.DataScopeFilter;
import com.example.student.security.DataScopeService;
import com.example.student.service.HardshipFundingService;
import com.example.student.service.NotificationSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 资助（敏感表）：无数据范围的行整行剔除；有范围但非敏感级角色的行对 sensitive_note 脱敏。 */
@Service
public class HardshipFundingServiceImpl extends ServiceImpl<HardshipFundingMapper, HardshipFunding>
        implements HardshipFundingService {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private NotificationSender notificationSender;

    /** 全校权限（ADMIN/EDUCATION_ADMIN）可见敏感备注；其余有范围角色脱敏。 */
    private boolean canSeeSensitive(DataScopeFilter f) {
        return f.isUnrestricted();
    }

    @Override
    public Map<String, Object> getPage(long current, long size, Long studentId, String status) {
        DataScopeFilter filter = dataScopeService.currentFilter();
        LambdaQueryWrapper<HardshipFunding> w = new LambdaQueryWrapper<HardshipFunding>()
                .eq(studentId != null, HardshipFunding::getStudentId, studentId)
                .eq(status != null && !status.isBlank(), HardshipFunding::getStatus, status)
                .orderByDesc(HardshipFunding::getId);
        List<HardshipFunding> rows = new java.util.ArrayList<>(
                list(w.last("LIMIT " + (current - 1) * size + ", " + size)));

        rows.removeIf(f -> {
            Student s = studentMapper.selectById(f.getStudentId());
            // TEACHER 等无该生范围的角色：整行不可见
            return !(filter.isUnrestricted() || dataScopeService.canAccessStudent(s));
        });
        if (!canSeeSensitive(filter)) {
            // COUNSELOR/DEPARTMENT_LEADER 等：可见行但敏感字段脱敏
            rows.forEach(f -> f.setSensitiveNote(null));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("records", rows);
        return result;
    }

    @Override
    @Transactional
    public void addApplication(HardshipFunding funding) {
        if (funding.getStudentId() == null) throw new BusinessException("学生不能为空");
        if (funding.getAcademicYear() == null || funding.getAcademicYear().isBlank())
            throw new BusinessException("学年不能为空");
        Student student = studentMapper.selectById(funding.getStudentId());
        if (student == null) throw new BusinessException("学生不存在");
        if (!dataScopeService.canAccessStudent(student))
            throw new AccessDeniedException("无权为该学生提交资助申请");
        Long exists = count(new LambdaQueryWrapper<HardshipFunding>()
                .eq(HardshipFunding::getStudentId, funding.getStudentId())
                .eq(HardshipFunding::getAcademicYear, funding.getAcademicYear())
                .eq(funding.getFundingType() != null, HardshipFunding::getFundingType, funding.getFundingType()));
        if (exists != null && exists > 0)
            throw new BusinessException("该生本学年已存在同类型申请");
        funding.setId(null);
        if (funding.getStatus() == null) funding.setStatus("PENDING");
        save(funding);
    }

    @Override
    @Transactional
    public void review(Long id, boolean pass, String comment) {
        HardshipFunding f = getById(id);
        if (f == null) throw new BusinessException("申请不存在");
        Student student = studentMapper.selectById(f.getStudentId());
        DataScopeFilter filter = dataScopeService.currentFilter();
        if (!filter.isUnrestricted() && !dataScopeService.canAccessStudent(student))
            throw new AccessDeniedException("无权审核该申请");
        if (!"PENDING".equals(f.getStatus())) throw new BusinessException("该申请已处理");
        f.setStatus(pass ? "APPROVED" : "REJECTED");
        f.setReviewComment(comment);
        updateById(f);
        // P2-D 通知：审批结果通知提交学生
        notificationSender.toStudent(student, "FUND",
                pass ? "资助申请审批通过" : "资助申请审批驳回",
                "您的" + (f.getAcademicYear() == null ? "" : f.getAcademicYear() + " ")
                        + (f.getFundingType() == null ? "资助申请" : f.getFundingType())
                        + "已" + (pass ? "通过" : "驳回") + (pass ? "，资助金额 " + f.getAmount() : ""),
                f.getId());
    }
}
