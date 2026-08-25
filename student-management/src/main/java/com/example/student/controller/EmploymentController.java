package com.example.student.controller;

import com.example.student.common.PageQuery;
import com.example.student.common.Result;
import com.example.student.entity.EmploymentRecord;
import com.example.student.entity.StudentCertificate;
import com.example.student.service.EmploymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "就业与证书")
@RestController
@RequestMapping("/api/employment")
public class EmploymentController {

    @Autowired
    private EmploymentService employmentService;

    @Operation(summary = "就业记录列表")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','STUDENT')")
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@Valid @ModelAttribute PageQuery pageQuery,
                                            @RequestParam(required = false) Integer graduationYear,
                                            @RequestParam(required = false) String destinationType) {
        return Result.success(employmentService.getPage(pageQuery.getCurrent(), pageQuery.getSize(), graduationYear, destinationType));
    }

    @Operation(summary = "去向统计（按毕业年度×类型聚合）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','SCHOOL_LEADER')")
    @GetMapping("/stats")
    public Result<List<Map<String, Object>>> stats(@RequestParam(required = false) Integer graduationYear) {
        return Result.success(employmentService.destinationStats(graduationYear));
    }

    @Operation(summary = "登记就业去向")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','COUNSELOR','STUDENT')")
    @PostMapping
    public Result<Void> record(@RequestBody EmploymentRecord record) {
        employmentService.record(record);
        return Result.success();
    }

    @Operation(summary = "核验就业记录")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','COUNSELOR')")
    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        employmentService.confirm(id);
        return Result.success();
    }

    @Operation(summary = "证书列表（学生仅本人）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','STUDENT')")
    @GetMapping("/certificate/page")
    public Result<Map<String, Object>> certPage(@Valid @ModelAttribute PageQuery pageQuery,
                                                @RequestParam(required = false) Long studentId) {
        return Result.success(employmentService.certPage(pageQuery.getCurrent(), pageQuery.getSize(), studentId));
    }

    @Operation(summary = "登记证书")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','COUNSELOR','STUDENT')")
    @PostMapping("/certificate")
    public Result<Void> addCert(@RequestBody StudentCertificate cert) {
        employmentService.addCertificate(cert);
        return Result.success();
    }
}
