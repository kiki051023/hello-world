package com.example.student.controller;

import com.alibaba.excel.EasyExcel;
import com.example.student.common.ImportResult;
import com.example.student.common.Result;
import com.example.student.entity.Teacher;
import com.example.student.entity.ImportTask;
import com.example.student.excel.TeacherExcel;
import com.example.student.exception.BusinessException;
import com.example.student.service.TeacherService;
import com.example.student.service.ImportTaskService;
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

@Tag(name = "教师管理")
@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private ImportTaskService importTaskService;

    @Operation(summary = "所有教师列表")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping("/list")
    public Result<List<Teacher>> getAllTeacher() {
        return Result.success(teacherService.getAllTeacher());
    }

    @Operation(summary = "教师详情")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping("/{id}")
    public Result<Teacher> getTeacherById(@PathVariable Long id) {
        return Result.success(teacherService.getById(id));
    }

    @Operation(summary = "新增教师")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Result<Void> addTeacher(@RequestBody Teacher teacher) {
        teacherService.addTeacher(teacher);
        return Result.success();
    }

    @Operation(summary = "修改教师")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public Result<Void> updateTeacher(@RequestBody Teacher teacher) {
        teacherService.updateTeacher(teacher);
        return Result.success();
    }

    @Operation(summary = "删除教师")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return Result.success();
    }

    @Operation(summary = "导出教师(Excel)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/export")
    public void exportTeachers(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("教师信息", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), TeacherExcel.class)
                .sheet("教师信息").doWrite(teacherService.exportTeachers());
    }

    @Operation(summary = "导入教师(Excel)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import")
    public Result<ImportResult<TeacherExcel>> importTeachers(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        ImportTask task = importTaskService.start("teacher", file.getOriginalFilename(), null);
        try {
            ImportResult<TeacherExcel> result = teacherService.importTeachers(file);
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
    public void exportImportFail(@RequestBody List<TeacherExcel> failRows, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("教师导入失败明细", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), TeacherExcel.class).sheet("导入失败明细").doWrite(failRows);
    }
}
