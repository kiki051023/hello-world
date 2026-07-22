package com.example.student.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.Course;

import java.util.List;

public interface CourseService extends IService<Course> {

    IPage<Course> getCoursePage(Long current, Long size, String courseName, Long teacherId);

    void addCourse(Course course);

    void updateCourse(Course course);

    void deleteCourse(Long id);

    List<Course> getAllCourse();
}
