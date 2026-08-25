package com.example.student.controller;

import com.example.student.common.PageQuery;
import com.example.student.common.Result;
import com.example.student.entity.HardshipFunding;
import com.example.student.service.HardshipFundingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "困难资助")
@RestController
@RequestMapping("/api/funding")
public class HardshipFundingController {

    @Autowired
    private HardshipFundingService hardshipFundingService;

    @Operation(summary = "资助列表（敏感字段按角色脱敏）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','STUDENT')")
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@Valid @ModelAttribute PageQuery pageQuery,
                                            @RequestParam(required = false) Long studentId,
                                            @RequestParam(required = false) String status) {
        return Result.success(hardshipFundingService.getPage(pageQuery.getCurrent(), pageQuery.getSize(), studentId, status));
    }

    @Operation(summary = "提交资助申请")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','COUNSELOR','STUDENT')")
    @PostMapping
    public Result<Void> add(@RequestBody HardshipFunding funding) {
        hardshipFundingService.addApplication(funding);
        return Result.success();
    }

    @Operation(summary = "审核")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR')")
    @PostMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @RequestParam boolean pass,
                               @RequestParam(required = false) String comment) {
        hardshipFundingService.review(id, pass, comment);
        return Result.success();
    }
}
