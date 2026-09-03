package com.example.student.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.CourseSelection;

import java.util.Map;

public interface CourseSelectionService extends IService<CourseSelection> {

    Map<String, Object> getPage(long current, long size, Long courseTermId, Long studentId);

    /** 学生选课：容量校验、重复校验，学生仅能选本人。 */
    void select(CourseSelection selection);

    void cancel(Long id);

    /** M4-2 学生周课表（学生看本人，管理端可指定 studentId） */
    java.util.List<java.util.Map<String, Object>> studentTimetable(Long studentId);
}
