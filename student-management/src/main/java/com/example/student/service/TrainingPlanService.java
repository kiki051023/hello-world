package com.example.student.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.TrainingPlan;
import com.example.student.entity.TrainingPlanCourse;

import java.util.List;
import java.util.Map;

public interface TrainingPlanService extends IService<TrainingPlan> {

    IPage<TrainingPlan> getPage(long current, long size, Long majorId, Integer enrollmentYear);

    TrainingPlan getDetail(Long id);

    List<Map<String, Object>> listCourses(Long planId);

    void addPlan(TrainingPlan plan);

    void updatePlan(TrainingPlan plan);

    void deletePlan(Long id);

    void saveCourses(Long planId, List<TrainingPlanCourse> courses);
}
