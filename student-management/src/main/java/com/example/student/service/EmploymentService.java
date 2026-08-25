package com.example.student.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.EmploymentRecord;
import com.example.student.entity.StudentCertificate;

import java.util.List;
import java.util.Map;

public interface EmploymentService extends IService<EmploymentRecord> {

    Map<String, Object> getPage(long current, long size, Integer graduationYear, String destinationType);

    void record(EmploymentRecord record);

    void confirm(Long id);

    /** 按毕业年度×去向类型聚合统计（单条 SQL） */
    List<Map<String, Object>> destinationStats(Integer graduationYear);

    // ===== 证书 =====
    Map<String, Object> certPage(long current, long size, Long studentId);

    void addCertificate(StudentCertificate cert);
}
