package com.example.student.controller;

import com.example.student.common.Result;
import com.example.student.entity.Teacher;
import com.example.student.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "教师管理")
@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    @Autowired
    private TeacherService teacherService;

    @Operation(summary = "所有教师列表")
    @GetMapping("/list")
    public Result<List<Teacher>> getAllTeacher() {
        return Result.success(teacherService.getAllTeacher());
    }

    @Operation(summary = "教师详情")
    @GetMapping("/{id}")
    public Result<Teacher> getTeacherById(@PathVariable Long id) {
        return Result.success(teacherService.getById(id));
    }

    @Operation(summary = "新增教师")
    @PostMapping
    public Result<Void> addTeacher(@RequestBody Teacher teacher) {
        teacherService.addTeacher(teacher);
        return Result.success();
    }

    @Operation(summary = "修改教师")
    @PutMapping
    public Result<Void> updateTeacher(@RequestBody Teacher teacher) {
        teacherService.updateTeacher(teacher);
        return Result.success();
    }

    @Operation(summary = "删除教师")
    @DeleteMapping("/{id}")
    public Result<Void> deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return Result.success();
    }
}
