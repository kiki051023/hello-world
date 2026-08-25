package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.student.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    IPage<Course> selectCoursePage(Page<Course> page, @Param("courseName") String courseName,
                                   @Param("teacherId") Long teacherId);

    List<Map<String, Object>> selectCourseExport(@Param("courseName") String courseName,
                                                 @Param("teacherId") Long teacherId);

    List<Map<String, Object>> selectIdsByCourseNames(@Param("courseNames") List<String> courseNames);
}
