package com.example.student.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.common.ImportResult;
import com.example.student.common.PageResult;
import com.example.student.common.Result;
import com.example.student.entity.Course;
import com.example.student.excel.CourseExcel;
import com.example.student.exception.BusinessException;
import com.example.student.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "课程管理")
@RestController
@RequestMapping("/api/course")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Operation(summary = "课程列表（分页）")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
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
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    @GetMapping("/list")
    public Result<List<Course>> getAllCourse() {
        return Result.success(courseService.getAllCourse());
    }

    @Operation(summary = "课程详情")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    @GetMapping("/{id}")
    public Result<Course> getCourseById(@PathVariable Long id) {
        return Result.success(courseService.getById(id));
    }

    @Operation(summary = "新增课程")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Result<Void> addCourse(@RequestBody Course course) {
        courseService.addCourse(course);
        return Result.success();
    }

    @Operation(summary = "修改课程")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public Result<Void> updateCourse(@RequestBody Course course) {
        courseService.updateCourse(course);
        return Result.success();
    }

    @Operation(summary = "删除课程")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return Result.success();
    }

    @Operation(summary = "导出课程(Excel)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/export")
    public void exportCourses(HttpServletResponse response,
                              @RequestParam(required = false) String courseName,
                              @RequestParam(required = false) Long teacherId) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("课程信息", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), CourseExcel.class)
                .sheet("课程信息").doWrite(courseService.exportCourses(courseName, teacherId));
    }

    @Operation(summary = "导入课程(Excel)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import")
    public Result<ImportResult<CourseExcel>> importCourses(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        return Result.success(courseService.importCourses(file));
    }

    @Operation(summary = "导出导入失败明细(Excel)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import-fail-export")
    public void exportImportFail(@RequestBody List<CourseExcel> failRows, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("课程导入失败明细", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), CourseExcel.class).sheet("导入失败明细").doWrite(failRows);
    }
}
