package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.EmploymentRecord;
import com.example.student.entity.Student;
import com.example.student.entity.StudentCertificate;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.EmploymentRecordMapper;
import com.example.student.mapper.StudentCertificateMapper;
import com.example.student.mapper.StudentMapper;
import com.example.student.security.DataScopeService;
import com.example.student.security.StudentDataScopeService;
import com.example.student.service.EmploymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmploymentServiceImpl extends ServiceImpl<EmploymentRecordMapper, EmploymentRecord>
        implements EmploymentService {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private StudentCertificateMapper certMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private StudentDataScopeService studentDataScopeService;

    @Lazy
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> getPage(long current, long size, Integer graduationYear, String destinationType) {
        LambdaQueryWrapper<EmploymentRecord> w = new LambdaQueryWrapper<EmploymentRecord>()
                .eq(graduationYear != null, EmploymentRecord::getGraduationYear, graduationYear)
                .eq(destinationType != null && !destinationType.isBlank(),
                        EmploymentRecord::getDestinationType, destinationType)
                .orderByDesc(EmploymentRecord::getId);
        List<EmploymentRecord> rows = new ArrayList<>(
                list(w.last("LIMIT " + (current - 1) * size + ", " + size)));
        rows.removeIf(r -> !dataScopeService.canAccessStudent(studentMapper.selectById(r.getStudentId())));
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("records", rows);
        return result;
    }

    @Override
    @Transactional
    public void record(EmploymentRecord record) {
        if (record.getStudentId() == null || record.getGraduationYear() == null
                || record.getDestinationType() == null)
            throw new BusinessException("学生、毕业年度与去向类型不能为空");
        if (!dataScopeService.currentFilter().isUnrestricted()
                && !dataScopeService.canAccessStudent(studentMapper.selectById(record.getStudentId())))
            throw new AccessDeniedException("无权登记该学生就业信息");
        Long exists = count(new LambdaQueryWrapper<EmploymentRecord>()
                .eq(EmploymentRecord::getStudentId, record.getStudentId())
                .eq(EmploymentRecord::getGraduationYear, record.getGraduationYear()));
        if (exists != null && exists > 0)
            throw new BusinessException("该生该毕业年度已存在就业记录");
        record.setId(null);
        save(record);
    }

    @Override
    @Transactional
    public void confirm(Long id) {
        EmploymentRecord r = getById(id);
        if (r == null) throw new BusinessException("记录不存在");
        r.setConfirmed(1);
        updateById(r);
    }

    @Override
    public List<Map<String, Object>> destinationStats(Integer graduationYear) {
        // 单条聚合 SQL：毕业年度×去向类型 人数统计（含表名白名单，无注入面）
        String sql = "SELECT destination_type AS destinationType, COUNT(*) AS cnt"
                + " FROM employment_record WHERE deleted = 0"
                + (graduationYear != null ? " AND graduation_year = ?" : "")
                + " GROUP BY destination_type ORDER BY cnt DESC";
        return graduationYear != null
                ? jdbcTemplate.queryForList(sql, graduationYear)
                : jdbcTemplate.queryForList(sql);
    }

    @Override
    public Map<String, Object> certPage(long current, long size, Long studentId) {
        Long scoped = studentDataScopeService.resolveStudentId(studentId);
        LambdaQueryWrapper<StudentCertificate> w = new LambdaQueryWrapper<StudentCertificate>()
                .eq(scoped != null, StudentCertificate::getStudentId, scoped)
                .orderByDesc(StudentCertificate::getId);
        List<StudentCertificate> rows = new ArrayList<>(certMapper.selectList(w));
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("records", rows);
        return result;
    }

    @Override
    @Transactional
    public void addCertificate(StudentCertificate cert) {
        if (cert.getStudentId() == null || cert.getCertName() == null || cert.getCertName().isBlank())
            throw new BusinessException("学生与证书名称不能为空");
        if (!dataScopeService.currentFilter().isUnrestricted()
                && !dataScopeService.canAccessStudent(studentMapper.selectById(cert.getStudentId())))
            throw new AccessDeniedException("无权为该学生登记证书");
        cert.setId(null);
        certMapper.insert(cert);
    }
}
