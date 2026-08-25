package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.student.entity.TrainingPlanCourse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface TrainingPlanCourseMapper extends BaseMapper<TrainingPlanCourse> {

    List<Map<String, Object>> selectByPlanId(@Param("planId") Long planId);
}
