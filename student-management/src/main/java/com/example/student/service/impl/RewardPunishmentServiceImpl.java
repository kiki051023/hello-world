package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.RewardPunishment;
import com.example.student.entity.Student;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.RewardPunishmentMapper;
import com.example.student.mapper.StudentMapper;
import com.example.student.security.DataScopeService;
import com.example.student.service.RewardPunishmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RewardPunishmentServiceImpl extends ServiceImpl<RewardPunishmentMapper, RewardPunishment>
        implements RewardPunishmentService {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Override
    public Map<String, Object> getPage(long current, long size, Long studentId, String rpType) {
        LambdaQueryWrapper<RewardPunishment> w = new LambdaQueryWrapper<RewardPunishment>()
                .eq(studentId != null, RewardPunishment::getStudentId, studentId)
                .eq(rpType != null && !rpType.isBlank(), RewardPunishment::getRpType, rpType)
                .orderByDesc(RewardPunishment::getId);
        List<RewardPunishment> rows = new java.util.ArrayList<>(
                list(w.last("LIMIT " + (current - 1) * size + ", " + size)));
        // 数据范围过滤（列表量小，内存收窄；越权行直接剔除）
        rows.removeIf(r -> {
            Student s = studentMapper.selectById(r.getStudentId());
            return !dataScopeService.canAccessStudent(s);
        });
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("records", rows);
        return result;
    }

    @Override
    @Transactional
    public void addRecord(RewardPunishment record) {
        if (record.getStudentId() == null) throw new BusinessException("学生不能为空");
        Student student = studentMapper.selectById(record.getStudentId());
        if (student == null) throw new BusinessException("学生不存在");
        if (!dataScopeService.canAccessStudent(student))
            throw new AccessDeniedException("无权为该学生登记奖惩");
        if (!"REWARD".equals(record.getRpType()) && !"PUNISHMENT".equals(record.getRpType()))
            throw new BusinessException("类型必须为 REWARD 或 PUNISHMENT");
        record.setId(null);
        record.setRevoked(0);
        save(record);
    }

    @Override
    @Transactional
    public void revoke(Long id, String note) {
        RewardPunishment rp = getById(id);
        if (rp == null) throw new BusinessException("记录不存在");
        if (!"PUNISHMENT".equals(rp.getRpType())) throw new BusinessException("仅处分可解除");
        if (Integer.valueOf(1).equals(rp.getRevoked())) throw new BusinessException("该处分已解除");
        rp.setRevoked(1);
        rp.setRevokeDate(LocalDate.now());
        if (note != null && !note.isBlank()) {
            String desc = rp.getDescription() == null ? "" : rp.getDescription() + "；";
            rp.setDescription(desc + "解除说明：" + note);
        }
        updateById(rp);
    }
}
