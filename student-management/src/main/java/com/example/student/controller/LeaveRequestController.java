package com.example.student.controller;

import com.example.student.common.PageQuery;
import com.example.student.common.Result;
import com.example.student.entity.LeaveRequest;
import com.example.student.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "学生请假")
@RestController
@RequestMapping("/api/leave")
public class LeaveRequestController {

    @Autowired
    private LeaveRequestService leaveRequestService;

    @Operation(summary = "请假列表（学生仅本人）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','STUDENT')")
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@Valid @ModelAttribute PageQuery pageQuery,
                                            @RequestParam(required = false) Long studentId,
                                            @RequestParam(required = false) String status) {
        return Result.success(leaveRequestService.getPage(pageQuery.getCurrent(), pageQuery.getSize(), studentId, status));
    }

    @Operation(summary = "提交请假")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','STUDENT')")
    @PostMapping
    public Result<Void> submit(@RequestBody LeaveRequest request) {
        leaveRequestService.submit(request);
        return Result.success();
    }

    @Operation(summary = "审批")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR')")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestParam boolean pass,
                                @RequestParam(required = false) String comment) {
        leaveRequestService.approve(id, pass, comment);
        return Result.success();
    }

    @Operation(summary = "销假")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER')")
    @PostMapping("/{id}/return")
    public Result<Void> returnBack(@PathVariable Long id) {
        leaveRequestService.returnBack(id);
        return Result.success();
    }
}
