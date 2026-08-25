package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.TrainingPlan;
import com.example.student.entity.TrainingPlanCourse;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.TrainingPlanCourseMapper;
import com.example.student.mapper.TrainingPlanMapper;
import com.example.student.service.TrainingPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TrainingPlanServiceImpl extends ServiceImpl<TrainingPlanMapper, TrainingPlan>
        implements TrainingPlanService {

    @Autowired
    private TrainingPlanCourseMapper planCourseMapper;

    @Override
    public IPage<TrainingPlan> getPage(long current, long size, Long majorId, Integer enrollmentYear) {
        LambdaQueryWrapper<TrainingPlan> wrapper = new LambdaQueryWrapper<TrainingPlan>()
                .eq(majorId != null, TrainingPlan::getMajorId, majorId)
                .eq(enrollmentYear != null, TrainingPlan::getEnrollmentYear, enrollmentYear)
                .orderByDesc(TrainingPlan::getId);
        return page(new Page<>(current, size), wrapper);
    }

    @Override
    public TrainingPlan getDetail(Long id) {
        return getById(id);
    }

    @Override
    public List<Map<String, Object>> listCourses(Long planId) {
        return planCourseMapper.selectByPlanId(planId);
    }

    @Override
    @Transactional
    public void addPlan(TrainingPlan plan) {
        requireUnique(plan);
        baseMapper.insert(plan);
    }

    @Override
    @Transactional
    public void updatePlan(TrainingPlan plan) {
        if (baseMapper.selectById(plan.getId()) == null) throw new BusinessException("培养方案不存在");
        requireUnique(plan);
        baseMapper.updateById(plan);
    }

    @Override
    @Transactional
    public void deletePlan(Long id) {
        if (baseMapper.selectById(id) == null) throw new BusinessException("培养方案不存在");
        removeById(id);
    }

    @Override
    @Transactional
    public void saveCourses(Long planId, List<TrainingPlanCourse> courses) {
        if (baseMapper.selectById(planId) == null) throw new BusinessException("培养方案不存在");
        if (courses != null && courses.size() > 500) throw new BusinessException("课程数超出限制(500)");
        planCourseMapper.delete(new LambdaQueryWrapper<TrainingPlanCourse>()
                .eq(TrainingPlanCourse::getPlanId, planId));
        if (courses != null) {
            for (TrainingPlanCourse c : courses) {
                c.setId(null);
                c.setPlanId(planId);
            }
            // 培养方案内课程不允许重复
            long distinct = courses.stream().map(TrainingPlanCourse::getCourseId).distinct().count();
            if (distinct != courses.size()) throw new BusinessException("培养方案内存在重复课程");
            for (TrainingPlanCourse c : courses) planCourseMapper.insert(c);
        }
    }

    private void requireUnique(TrainingPlan plan) {
        if (plan.getMajorId() == null || plan.getEnrollmentYear() == null)
            throw new BusinessException("专业和入学年份不能为空");
        Long exists = baseMapper.selectCount(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getMajorId, plan.getMajorId())
                .eq(TrainingPlan::getEnrollmentYear, plan.getEnrollmentYear())
                .ne(plan.getId() != null, TrainingPlan::getId, plan.getId()));
        if (exists != null && exists > 0)
            throw new BusinessException("该专业同年份的培养方案已存在");
    }
}
