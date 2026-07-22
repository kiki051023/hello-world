package com.example.student.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.common.PageResult;
import com.example.student.common.Result;
import com.example.student.entity.Student;
import com.example.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "学生管理")
@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Operation(summary = "学生列表（分页）")
    @GetMapping("/page")
    public Result<PageResult<Student>> getStudentPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String studentNo,
            @RequestParam(required = false) Long clazzId) {
        IPage<Student> page = studentService.getStudentPage(current, size, name, studentNo, clazzId);
        return Result.success(PageResult.of(page.getTotal(), page.getRecords(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "学生详情")
    @GetMapping("/{id}")
    public Result<Student> getStudentById(@PathVariable Long id) {
        return Result.success(studentService.getStudentById(id));
    }

    @Operation(summary = "新增学生")
    @PostMapping
    public Result<Void> addStudent(@RequestBody Student student) {
        studentService.addStudent(student);
        return Result.success();
    }

    @Operation(summary = "修改学生")
    @PutMapping
    public Result<Void> updateStudent(@RequestBody Student student) {
        studentService.updateStudent(student);
        return Result.success();
    }

    @Operation(summary = "删除学生")
    @DeleteMapping("/{id}")
    public Result<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return Result.success();
    }
}
