package com.example.student.controller;

import com.example.student.common.PageQuery;
import com.example.student.common.Result;
import com.example.student.entity.CourseSelection;
import com.example.student.service.CourseSelectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "学生选课")
@RestController
@RequestMapping("/api/course-selection")
public class CourseSelectionController {

    @Autowired
    private CourseSelectionService courseSelectionService;

    @Operation(summary = "选课列表（分页，学生仅见本人）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','STUDENT')")
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@Valid @ModelAttribute PageQuery pageQuery,
                                            @RequestParam(required = false) Long courseTermId,
                                            @RequestParam(required = false) Long studentId) {
        return Result.success(courseSelectionService.getPage(pageQuery.getCurrent(), pageQuery.getSize(),
                courseTermId, studentId));
    }

    @Operation(summary = "选课")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','TEACHER','STUDENT')")
    @PostMapping
    public Result<Void> select(@RequestBody CourseSelection selection) {
        courseSelectionService.select(selection);
        return Result.success();
    }

    @Operation(summary = "退选")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','TEACHER','STUDENT')")
    @DeleteMapping("/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        courseSelectionService.cancel(id);
        return Result.success();
    }
}
