package com.example.student.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.common.ImportResult;
import com.example.student.common.PageResult;
import com.example.student.common.PageQuery;
import com.example.student.common.Result;
import com.example.student.entity.Student;
import com.example.student.excel.StudentExcel;
import com.example.student.exception.BusinessException;
import com.example.student.service.StudentService;
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

@Tag(name = "学生管理")
@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Operation(summary = "学生列表（分页）")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping("/page")
    public Result<PageResult<Student>> getStudentPage(
            @Valid @ModelAttribute PageQuery pageQuery,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String studentNo,
            @RequestParam(required = false) Long clazzId) {
        IPage<Student> page = studentService.getStudentPage(pageQuery.getCurrent(), pageQuery.getSize(), name, studentNo, clazzId);
        return Result.success(PageResult.of(page.getTotal(), page.getRecords(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "学生详情")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping("/{id}")
    public Result<Student> getStudentById(@PathVariable Long id) {
        return Result.success(studentService.getStudentById(id));
    }

    @Operation(summary = "新增学生")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Result<Void> addStudent(@RequestBody Student student) {
        studentService.addStudent(student);
        return Result.success();
    }

    @Operation(summary = "修改学生")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public Result<Void> updateStudent(@RequestBody Student student) {
        studentService.updateStudent(student);
        return Result.success();
    }

    @Operation(summary = "删除学生")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return Result.success();
    }

    @Operation(summary = "导出学生(Excel)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/export")
    public void exportStudents(HttpServletResponse response,
                               @RequestParam(required = false) String name,
                               @RequestParam(required = false) String studentNo,
                               @RequestParam(required = false) Long clazzId) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("学生信息", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), StudentExcel.class)
                .sheet("学生信息").doWrite(studentService.exportStudents(name, studentNo, clazzId));
    }

    @Operation(summary = "导入学生(Excel)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import")
    public Result<ImportResult<StudentExcel>> importStudents(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        return Result.success(studentService.importStudents(file));
    }

    @Operation(summary = "导出导入失败明细(Excel)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import-fail-export")
    public void exportImportFail(@RequestBody List<StudentExcel> failRows, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("学生导入失败明细", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), StudentExcel.class).sheet("导入失败明细").doWrite(failRows);
    }
}
