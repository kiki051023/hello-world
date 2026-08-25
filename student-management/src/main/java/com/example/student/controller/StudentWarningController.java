package com.example.student.controller;

import com.example.student.common.PageQuery;
import com.example.student.common.Result;
import com.example.student.entity.StudentWarning;
import com.example.student.service.ScoreService;
import com.example.student.service.StudentWarningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "学生预警")
@RestController
@RequestMapping("/api/warning")
public class StudentWarningController {

    @Autowired
    private StudentWarningService studentWarningService;

    @Autowired
    private ScoreService scoreService;

    @Operation(summary = "预警列表")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER')")
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@Valid @ModelAttribute PageQuery pageQuery,
                                            @RequestParam(required = false) Long studentId,
                                            @RequestParam(required = false) String status) {
        return Result.success(studentWarningService.getPage(pageQuery.getCurrent(), pageQuery.getSize(), studentId, status));
    }

    @Operation(summary = "按规则生成成绩挂科预警（幂等）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR')")
    @PostMapping("/generate/score-fail")
    public Result<Map<String, Object>> generateScoreFail(@RequestParam(defaultValue = "3") int threshold) {
        // 单条聚合查询：每生有效不及格课程数（active=1 且 APPROVED，每课取最高分）
        Map<Long, Integer> failCounts = scoreService.countFailCoursesPerStudent();
        int created = studentWarningService.generateByRule("SCORE", "score_fail_" + threshold, threshold, failCounts);
        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        result.put("scanned", failCounts.size());
        result.put("threshold", threshold);
        return Result.success(result);
    }

    @Operation(summary = "处理/驳回预警")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR')")
    @PostMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean dismiss,
                               @RequestParam(required = false) String note) {
        studentWarningService.handle(id, dismiss, note);
        return Result.success();
    }
}
