package com.example.student.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.PracticeAssignment;

import java.util.Map;

public interface PracticeAssignmentService extends IService<PracticeAssignment> {

    /** 列表：指导教师仅看本人负责学生；学生仅看本人 */
    Map<String, Object> getPage(long current, long size, Long planId, Long studentId);

    PracticeAssignment getByIdChecked(Long id);

    void assign(PracticeAssignment assignment);

    void finish(Long id, java.math.BigDecimal score);
}
