package com.example.student.controller;

import com.example.student.common.PageQuery;
import com.example.student.common.Result;
import com.example.student.entity.PracticeAssignment;
import com.example.student.service.PracticeAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "实习分配")
@RestController
@RequestMapping("/api/practice-assignment")
public class PracticeAssignmentController {

    @Autowired
    private PracticeAssignmentService assignmentService;

    @Operation(summary = "实习分配列表（教师仅见负责学生，学生仅本人）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER','STUDENT')")
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@Valid @ModelAttribute PageQuery pageQuery,
                                            @RequestParam(required = false) Long planId,
                                            @RequestParam(required = false) Long studentId) {
        return Result.success(assignmentService.getPage(pageQuery.getCurrent(), pageQuery.getSize(), planId, studentId));
    }

    @Operation(summary = "详情")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER','STUDENT')")
    @GetMapping("/{id}")
    public Result<PracticeAssignment> getById(@PathVariable Long id) {
        return Result.success(assignmentService.getByIdChecked(id));
    }

    @Operation(summary = "建立实习分配（含校内指导关系）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','TEACHER','PRACTICE_TEACHER')")
    @PostMapping
    public Result<Void> assign(@RequestBody PracticeAssignment assignment) {
        assignmentService.assign(assignment);
        return Result.success();
    }

    @Operation(summary = "结束实习并评定成绩")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','TEACHER','PRACTICE_TEACHER')")
    @PostMapping("/{id}/finish")
    public Result<Void> finish(@PathVariable Long id, @RequestParam BigDecimal score) {
        assignmentService.finish(id, score);
        return Result.success();
    }
}
