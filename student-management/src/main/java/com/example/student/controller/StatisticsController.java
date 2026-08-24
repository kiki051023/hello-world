package com.example.student.controller;

import com.example.student.common.Result;
import com.example.student.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "统计分析 / 数据大屏")
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @Operation(summary = "数据概览")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(statisticsService.getOverview());
    }

    @Operation(summary = "成绩深度分析（分布/班级/课程/Top/趋势/及格率）")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    @GetMapping("/score-analysis")
    public Result<Map<String, Object>> scoreAnalysis() {
        return Result.success(statisticsService.getScoreAnalysis());
    }

    @Operation(summary = "单个学生各课程成绩雷达")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    @GetMapping("/student-radar/{studentId}")
    public Result<Map<String, Object>> studentRadar(@PathVariable Long studentId) {
        return Result.success(statisticsService.getStudentRadar(studentId));
    }
}
