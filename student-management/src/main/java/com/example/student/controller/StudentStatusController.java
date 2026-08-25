package com.example.student.controller;

import com.example.student.common.PageQuery;
import com.example.student.common.Result;
import com.example.student.entity.StudentStatusHistory;
import com.example.student.service.StudentStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "学籍异动")
@RestController
@RequestMapping("/api/student-status")
public class StudentStatusController {

    @Autowired
    private StudentStatusService studentStatusService;

    @Operation(summary = "学籍异动（转专业/转班/休学/复学/毕业/状态）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR')")
    @PostMapping("/{studentId}/change")
    public Result<Void> change(@PathVariable Long studentId,
                               @RequestParam String changeType,
                               @RequestParam String newValue,
                               @RequestParam(required = false) String oldValue,
                               @RequestParam(required = false) String reason) {
        studentStatusService.applyChange(studentId, changeType, oldValue, newValue, reason);
        return Result.success();
    }

    @Operation(summary = "某学生的异动历史")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','STUDENT')")
    @GetMapping("/{studentId}/history")
    public Result<List<StudentStatusHistory>> history(@PathVariable Long studentId) {
        return Result.success(studentStatusService.history(studentId));
    }

    @Operation(summary = "异动历史分页")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR')")
    @GetMapping("/history/page")
    public Result<Map<String, Object>> historyPage(@Valid @ModelAttribute PageQuery pageQuery,
                                                   @RequestParam(required = false) Long studentId) {
        return Result.success(studentStatusService.historyPage(pageQuery.getCurrent(), pageQuery.getSize(), studentId));
    }
}
