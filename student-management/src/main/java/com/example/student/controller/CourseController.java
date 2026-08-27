package com.example.student.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.common.ImportResult;
import com.example.student.common.PageResult;
import com.example.student.common.PageQuery;
import com.example.student.common.Result;
import com.example.student.entity.Course;
import com.example.student.dto.CourseDTO;
import com.example.student.dto.ApiDtoMapper;
import com.example.student.entity.ImportTask;
import com.example.student.excel.CourseExcel;
import com.example.student.exception.BusinessException;
import com.example.student.service.CourseService;
import com.example.student.service.ImportTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
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

    @Autowired
    private ImportTaskService importTaskService;

    @Autowired
    private com.example.student.service.AsyncExportService asyncExportService;

    @Operation(summary = "课程列表（分页）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER','STUDENT')")
    @GetMapping("/page")
    public Result<PageResult<CourseDTO>> getCoursePage(
            @Valid @ModelAttribute PageQuery pageQuery,
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) Long teacherId) {
        IPage<Course> page = courseService.getCoursePage(pageQuery.getCurrent(), pageQuery.getSize(), courseName, teacherId);
        return Result.success(PageResult.of(page.getTotal(), page.getRecords().stream().map(ApiDtoMapper::course).toList(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "所有课程列表")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER','STUDENT')")
    @GetMapping("/list")
    public Result<List<CourseDTO>> getAllCourse() {
        return Result.success(courseService.getAllCourse().stream().map(ApiDtoMapper::course).toList());
    }

    @Operation(summary = "课程详情")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER','STUDENT')")
    @GetMapping("/{id}")
    public Result<CourseDTO> getCourseById(@PathVariable Long id) {
        Course course = courseService.getCourseById(id);
        return Result.success(course == null ? null : ApiDtoMapper.course(course));
    }

    @Operation(summary = "新增课程")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER')")
    @PostMapping
    public Result<Void> addCourse(@RequestBody CourseDTO course) {
        courseService.addCourse(ApiDtoMapper.course(course));
        return Result.success();
    }

    @Operation(summary = "修改课程")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER')")
    @PutMapping
    public Result<Void> updateCourse(@RequestBody CourseDTO course) {
        courseService.updateCourse(ApiDtoMapper.course(course));
        return Result.success();
    }

    @Operation(summary = "删除课程")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER')")
    @DeleteMapping("/{id}")
    public Result<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return Result.success();
    }

    @Operation(summary = "导出课程(Excel)；超10,000行转异步任务")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER')")
    @GetMapping("/export")
    public Object exportCourses(HttpServletResponse response,
                                @RequestParam(required = false) String courseName,
                                @RequestParam(required = false) Long teacherId) throws IOException {
        long total = courseService.countForExport(courseName, teacherId);
        if (total > com.example.student.common.ImportLimits.MAX_EXPORT_ROWS) {
            String params = "courseName=" + courseName + ",teacherId=" + teacherId;
            com.example.student.common.ExportTask task = asyncExportService.submit(
                    "COURSE", params,
                    () -> courseService.exportCourses(courseName, teacherId).size());
            Map<String, Object> result = new HashMap<>();
            result.put("async", true);
            result.put("taskNo", task.getTaskNo());
            result.put("totalRows", total);
            return Result.success(result);
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("课程信息", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), CourseExcel.class)
                .sheet("课程信息").doWrite(courseService.exportCourses(courseName, teacherId));
        return null;
    }

    @Operation(summary = "异步导出任务状态查询")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER')")
    @GetMapping("/export/status/{taskNo}")
    public Result<Map<String, Object>> exportStatus(@PathVariable String taskNo) {
        return Result.success(asyncExportService.status(taskNo));
    }

    @Operation(summary = "导入课程(Excel)")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER')")
    @PostMapping("/import")
    public Result<ImportResult<CourseExcel>> importCourses(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        ImportTask task = importTaskService.start("course", file.getOriginalFilename(), null);
        try {
            ImportResult<CourseExcel> result = courseService.importCourses(file);
            importTaskService.complete(task.getId(), result.getTotal(), result.getSuccessCount(), result.getFailRows().size());
            return Result.success(result);
        } catch (RuntimeException ex) {
            importTaskService.fail(task.getId(), ex.getMessage());
            throw ex;
        }
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
