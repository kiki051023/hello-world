package com.example.student.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.common.PageResult;
import com.example.student.common.Result;
import com.example.student.entity.Score;
import com.example.student.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "成绩管理")
@RestController
@RequestMapping("/api/score")
public class ScoreController {

    @Autowired
    private ScoreService scoreService;

    @Operation(summary = "成绩列表（分页）")
    @GetMapping("/page")
    public Result<PageResult<Score>> getScorePage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long clazzId) {
        IPage<Score> page = scoreService.getScorePage(current, size, studentId, courseId, clazzId);
        return Result.success(PageResult.of(page.getTotal(), page.getRecords(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "成绩详情")
    @GetMapping("/{id}")
    public Result<Score> getScoreById(@PathVariable Long id) {
        return Result.success(scoreService.getById(id));
    }

    @Operation(summary = "新增成绩")
    @PostMapping
    public Result<Void> addScore(@RequestBody Score score) {
        scoreService.addScore(score);
        return Result.success();
    }

    @Operation(summary = "修改成绩")
    @PutMapping
    public Result<Void> updateScore(@RequestBody Score score) {
        scoreService.updateScore(score);
        return Result.success();
    }

    @Operation(summary = "删除成绩")
    @DeleteMapping("/{id}")
    public Result<Void> deleteScore(@PathVariable Long id) {
        scoreService.deleteScore(id);
        return Result.success();
    }

    @Operation(summary = "成绩统计")
    @GetMapping("/stats")
    public Result<Map<String, BigDecimal>> getScoreStats(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long clazzId) {
        return Result.success(scoreService.getScoreStats(courseId, clazzId));
    }
}
