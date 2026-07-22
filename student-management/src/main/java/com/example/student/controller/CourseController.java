package com.example.student.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.common.PageResult;
import com.example.student.common.Result;
import com.example.student.entity.Course;
import com.example.student.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "课程管理")
@RestController
@RequestMapping("/api/course")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Operation(summary = "课程列表（分页）")
    @GetMapping("/page")
    public Result<PageResult<Course>> getCoursePage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) Long teacherId) {
        IPage<Course> page = courseService.getCoursePage(current, size, courseName, teacherId);
        return Result.success(PageResult.of(page.getTotal(), page.getRecords(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "所有课程列表")
    @GetMapping("/list")
    public Result<List<Course>> getAllCourse() {
        return Result.success(courseService.getAllCourse());
    }

    @Operation(summary = "课程详情")
    @GetMapping("/{id}")
    public Result<Course> getCourseById(@PathVariable Long id) {
        return Result.success(courseService.getById(id));
    }

    @Operation(summary = "新增课程")
    @PostMapping
    public Result<Void> addCourse(@RequestBody Course course) {
        courseService.addCourse(course);
        return Result.success();
    }

    @Operation(summary = "修改课程")
    @PutMapping
    public Result<Void> updateCourse(@RequestBody Course course) {
        courseService.updateCourse(course);
        return Result.success();
    }

    @Operation(summary = "删除课程")
    @DeleteMapping("/{id}")
    public Result<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return Result.success();
    }
}
