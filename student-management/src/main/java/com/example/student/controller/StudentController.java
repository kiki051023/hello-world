package com.example.student.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.common.ImportResult;
import com.example.student.common.PageResult;
import com.example.student.common.PageQuery;
import com.example.student.common.Result;
import com.example.student.entity.Student;
import com.example.student.dto.StudentDTO;
import com.example.student.dto.ApiDtoMapper;
import com.example.student.entity.ImportTask;
import com.example.student.excel.StudentExcel;
import com.example.student.exception.BusinessException;
import com.example.student.service.StudentService;
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

@Tag(name = "学生管理")
@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private ImportTaskService importTaskService;

    @Autowired
    private com.example.student.service.AsyncExportService asyncExportService;

    @Operation(summary = "学生列表（分页）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER','STUDENT')")
    @GetMapping("/page")
    public Result<PageResult<StudentDTO>> getStudentPage(
            @Valid @ModelAttribute PageQuery pageQuery,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String studentNo,
            @RequestParam(required = false) Long clazzId) {
        IPage<Student> page = studentService.getStudentPage(pageQuery.getCurrent(), pageQuery.getSize(), name, studentNo, clazzId);
        return Result.success(PageResult.of(page.getTotal(), page.getRecords().stream().map(ApiDtoMapper::student).toList(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "学生详情")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER','STUDENT')")
    @GetMapping("/{id}")
    public Result<StudentDTO> getStudentById(@PathVariable Long id) {
        Student student = studentService.getStudentById(id);
        return Result.success(student == null ? null : ApiDtoMapper.student(student));
    }

    @Operation(summary = "新增学生")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER')")
    @PostMapping
    public Result<Void> addStudent(@RequestBody StudentDTO student) {
        studentService.addStudent(ApiDtoMapper.student(student));
        return Result.success();
    }

    @Operation(summary = "修改学生")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER')")
    @PutMapping
    public Result<Void> updateStudent(@RequestBody StudentDTO student) {
        studentService.updateStudent(ApiDtoMapper.student(student));
        return Result.success();
    }

    @Operation(summary = "删除学生")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER')")
    @DeleteMapping("/{id}")
    public Result<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return Result.success();
    }

    @Operation(summary = "导出学生(Excel)；超10,000行转异步任务")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER')")
    @GetMapping("/export")
    public Object exportStudents(HttpServletResponse response,
                               @RequestParam(required = false) String name,
                               @RequestParam(required = false) String studentNo,
                               @RequestParam(required = false) Long clazzId) throws IOException {
        long total = studentService.countForExport(name, studentNo, clazzId);
        if (total > com.example.student.common.ImportLimits.MAX_EXPORT_ROWS) {
            String params = "name=" + name + ",studentNo=" + studentNo + ",clazzId=" + clazzId;
            com.example.student.common.ExportTask task = asyncExportService.submit(
                    "STUDENT", params,
                    () -> studentService.exportStudents(name, studentNo, clazzId).size());
            Map<String, Object> result = new HashMap<>();
            result.put("async", true);
            result.put("taskNo", task.getTaskNo());
            result.put("totalRows", total);
            return Result.success(result);
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("学生信息", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), StudentExcel.class)
                .sheet("学生信息").doWrite(studentService.exportStudents(name, studentNo, clazzId));
        return null;
    }

    @Operation(summary = "异步导出任务状态查询")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER')")
    @GetMapping("/export/status/{taskNo}")
    public Result<Map<String, Object>> exportStatus(@PathVariable String taskNo) {
        return Result.success(asyncExportService.status(taskNo));
    }

    @Operation(summary = "导入学生(Excel)")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER')")
    @PostMapping("/import")
    public Result<ImportResult<StudentExcel>> importStudents(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        ImportTask task = importTaskService.start("student", file.getOriginalFilename(), null);
        try {
            ImportResult<StudentExcel> result = studentService.importStudents(file);
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
    public void exportImportFail(@RequestBody List<StudentExcel> failRows, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("学生导入失败明细", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), StudentExcel.class).sheet("导入失败明细").doWrite(failRows);
    }
}
