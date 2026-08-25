package com.example.student.controller;

import com.example.student.common.Result;
import com.example.student.entity.AcademicTerm;
import com.example.student.service.AcademicTermService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "学年学期")
@RestController
@RequestMapping("/api/academic-term")
public class AcademicTermController {

    @Autowired
    private AcademicTermService academicTermService;

    @Operation(summary = "学期列表")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','STUDENT')")
    @GetMapping("/list")
    public Result<List<AcademicTerm>> list() {
        return Result.success(academicTermService.list());
    }

    @Operation(summary = "新增学期")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN')")
    @PostMapping
    public Result<Void> add(@RequestBody AcademicTerm term) {
        academicTermService.addTerm(term);
        return Result.success();
    }

    @Operation(summary = "修改学期")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN')")
    @PutMapping
    public Result<Void> update(@RequestBody AcademicTerm term) {
        academicTermService.updateTerm(term);
        return Result.success();
    }
}
