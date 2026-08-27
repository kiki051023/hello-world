package com.example.student.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.common.ImportResult;
import com.example.student.entity.Course;
import com.example.student.excel.CourseExcel;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CourseService extends IService<Course> {

    IPage<Course> getCoursePage(Long current, Long size, String courseName, Long teacherId);

    void addCourse(Course course);

    void updateCourse(Course course);

    void deleteCourse(Long id);

    List<Course> getAllCourse();
    Course getCourseById(Long id);

    List<CourseExcel> exportCourses(String courseName, Long teacherId);

    /** 课程导出行数（范围+筛选，聚合计数） */
    long countForExport(String courseName, Long teacherId);

    ImportResult<CourseExcel> importCourses(MultipartFile file);
}
