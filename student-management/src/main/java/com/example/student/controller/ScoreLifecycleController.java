package com.example.student.controller;

import com.example.student.common.Result;
import com.example.student.entity.Score;
import com.example.student.entity.ScoreChangeLog;
import com.example.student.service.ScoreLifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "成绩生命周期")
@RestController
@RequestMapping("/api/score-lifecycle")
public class ScoreLifecycleController {

    @Autowired
    private ScoreLifecycleService scoreLifecycleService;

    @Operation(summary = "录入草稿成绩")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','TEACHER')")
    @PostMapping("/draft")
    public Result<Score> createDraft(@RequestBody Score score,
                                     @RequestParam(required = false) String reason) {
        return Result.success(scoreLifecycleService.createDraft(score, reason));
    }

    @Operation(summary = "提交成绩")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','TEACHER')")
    @PostMapping("/{id}/submit")
    public Result<Score> submit(@PathVariable Long id) {
        return Result.success(scoreLifecycleService.submit(id));
    }

    @Operation(summary = "审核通过（同事务切换有效版本）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN')")
    @PostMapping("/{id}/approve")
    public Result<Score> approve(@PathVariable Long id) {
        return Result.success(scoreLifecycleService.approve(id));
    }

    @Operation(summary = "审核退回")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN')")
    @PostMapping("/{id}/reject")
    public Result<Score> reject(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return Result.success(scoreLifecycleService.reject(id, reason));
    }

    @Operation(summary = "作废成绩（不物理删除）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN')")
    @PostMapping("/{id}/void")
    public Result<Score> voidScore(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return Result.success(scoreLifecycleService.voidScore(id, reason));
    }

    @Operation(summary = "修改分数（保留变更历史）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','TEACHER')")
    @PutMapping("/{id}/value")
    public Result<Score> changeValue(@PathVariable Long id, @RequestParam BigDecimal newScore,
                                     @RequestParam(required = false) String reason) {
        return Result.success(scoreLifecycleService.changeScoreValue(id, newScore, reason));
    }

    @Operation(summary = "登记补考（独立记录）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','TEACHER')")
    @PostMapping("/{id}/makeup")
    public Result<Score> makeup(@PathVariable Long id, @RequestParam BigDecimal newScore,
                                @RequestParam(required = false) Long courseTermId) {
        return Result.success(scoreLifecycleService.createMakeup(id, newScore, courseTermId));
    }

    @Operation(summary = "登记重修（独立记录）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','TEACHER')")
    @PostMapping("/{id}/retake")
    public Result<Score> retake(@PathVariable Long id, @RequestParam BigDecimal newScore,
                                @RequestParam(required = false) Long courseTermId,
                                @RequestParam(required = false) Long newCourseTermId) {
        return Result.success(scoreLifecycleService.createRetake(id, newScore, courseTermId, newCourseTermId));
    }

    @Operation(summary = "成绩变更历史")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','STUDENT')")
    @GetMapping("/{id}/history")
    public Result<List<ScoreChangeLog>> history(@PathVariable Long id) {
        return Result.success(scoreLifecycleService.changeHistory(id));
    }

    @Operation(summary = "学生学分汇总（学生仅本人）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','STUDENT')")
    @GetMapping("/credit")
    public Result<Map<String, Object>> credit(@RequestParam(required = false) Long studentId,
                                              @RequestParam(required = false) Long termId) {
        return Result.success(scoreLifecycleService.creditSummary(studentId, termId));
    }

    @Operation(summary = "班级学分汇总（单条聚合查询）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER')")
    @GetMapping("/credit/clazz/{clazzId}")
    public Result<List<Map<String, Object>>> clazzCredit(@PathVariable Long clazzId,
                                                         @RequestParam(required = false) Long termId) {
        return Result.success(scoreLifecycleService.clazzCreditSummary(clazzId, termId));
    }
}
