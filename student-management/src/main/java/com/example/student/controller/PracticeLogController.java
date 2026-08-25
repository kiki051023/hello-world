package com.example.student.controller;

import com.example.student.common.PageQuery;
import com.example.student.common.Result;
import com.example.student.entity.PracticeLog;
import com.example.student.service.PracticeLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "实习周记")
@RestController
@RequestMapping("/api/practice-log")
public class PracticeLogController {

    @Autowired
    private PracticeLogService practiceLogService;

    @Operation(summary = "周记列表（按实习分配）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER','STUDENT')")
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@Valid @ModelAttribute PageQuery pageQuery,
                                            @RequestParam Long assignmentId,
                                            @RequestParam(required = false) String status) {
        return Result.success(practiceLogService.getPage(pageQuery.getCurrent(), pageQuery.getSize(), assignmentId, status));
    }

    @Operation(summary = "学生提交周记")
    @PreAuthorize("hasAnyRole('STUDENT')")
    @PostMapping
    public Result<Void> submit(@RequestBody PracticeLog log) {
        practiceLogService.submit(log);
        return Result.success();
    }

    @Operation(summary = "指导教师点评")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','TEACHER','PRACTICE_TEACHER')")
    @PostMapping("/{id}/comment")
    public Result<Void> comment(@PathVariable Long id, @RequestParam String comment) {
        practiceLogService.comment(id, comment);
        return Result.success();
    }
}
