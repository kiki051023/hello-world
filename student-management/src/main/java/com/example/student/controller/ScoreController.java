package com.example.student.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.common.ImportResult;
import com.example.student.common.PageResult;
import com.example.student.common.PageQuery;
import com.example.student.common.Result;
import com.example.student.entity.Score;
import com.example.student.dto.ScoreDTO;
import com.example.student.dto.ApiDtoMapper;
import com.example.student.entity.ImportTask;
import com.example.student.excel.ScoreExcel;
import com.example.student.exception.BusinessException;
import com.example.student.service.ScoreService;
import com.example.student.service.ImportTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "成绩管理")
@RestController
@RequestMapping("/api/score")
public class ScoreController {

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private ImportTaskService importTaskService;

    @Autowired
    private com.example.student.service.AsyncExportService asyncExportService;

    @Operation(summary = "成绩列表（分页）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER','STUDENT')")
    @GetMapping("/page")
    public Result<PageResult<ScoreDTO>> getScorePage(
            @Valid @ModelAttribute PageQuery pageQuery,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long clazzId,
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) String scoreStatus) {
        IPage<Score> page = scoreService.getScorePage(pageQuery.getCurrent(), pageQuery.getSize(), studentId, courseId, clazzId, termId, studentName, scoreStatus);
        return Result.success(PageResult.of(page.getTotal(), page.getRecords().stream().map(ApiDtoMapper::score).toList(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "成绩详情")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER','STUDENT')")
    @GetMapping("/{id}")
    public Result<ScoreDTO> getScoreById(@PathVariable Long id) {
        Score score = scoreService.getScoreById(id);
        return Result.success(score == null ? null : ApiDtoMapper.score(score));
    }

    @Operation(summary = "新增成绩")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER')")
    @PostMapping
    public Result<Void> addScore(@RequestBody ScoreDTO score) {
        scoreService.addScore(ApiDtoMapper.score(score));
        return Result.success();
    }

    @Operation(summary = "修改成绩")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER')")
    @PutMapping
    public Result<Void> updateScore(@RequestBody ScoreDTO score) {
        scoreService.updateScore(ApiDtoMapper.score(score));
        return Result.success();
    }

    @Operation(summary = "删除成绩")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER')")
    @DeleteMapping("/{id}")
    public Result<Void> deleteScore(@PathVariable Long id) {
        scoreService.deleteScore(id);
        return Result.success();
    }

    @Operation(summary = "成绩统计")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER','STUDENT')")
    @GetMapping("/stats")
    public Result<Map<String, BigDecimal>> getScoreStats(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long clazzId,
            @RequestParam(required = false) Long termId) {
        return Result.success(scoreService.getScoreStats(courseId, clazzId, termId));
    }

    @Operation(summary = "导出成绩(Excel)；超10,000行转异步任务")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER')")
    @GetMapping("/export")
    public Object exportScores(HttpServletResponse response,
                             @RequestParam(required = false) Long studentId,
                             @RequestParam(required = false) Long courseId,
                             @RequestParam(required = false) Long clazzId,
                             @RequestParam(required = false) Long termId) throws IOException {
        long total = scoreService.countForExport(studentId, courseId, clazzId, termId);
        if (total > com.example.student.common.ImportLimits.MAX_EXPORT_ROWS) {
            // 超阈值：转异步任务
            String params = "studentId=" + studentId + ",courseId=" + courseId + ",clazzId=" + clazzId + ",termId=" + termId;
            com.example.student.common.ExportTask task = asyncExportService.submit(
                    "SCORE", params,
                    () -> scoreService.exportScores(studentId, courseId, clazzId, termId).size());
            Map<String, Object> result = new HashMap<>();
            result.put("async", true);
            result.put("taskNo", task.getTaskNo());
            result.put("totalRows", total);
            return Result.success(result);
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("成绩信息", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), ScoreExcel.class)
                .sheet("成绩信息").doWrite(scoreService.exportScores(studentId, courseId, clazzId, termId));
        return null;
    }

    @Operation(summary = "异步导出任务状态查询")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER')")
    @GetMapping("/export/status/{taskNo}")
    public Result<Map<String, Object>> exportStatus(@PathVariable String taskNo) {
        return Result.success(asyncExportService.status(taskNo));
    }

    @Operation(summary = "导入成绩(Excel)")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER')")
    @PostMapping("/import")
    public Result<ImportResult<ScoreExcel>> importScores(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        ImportTask task = importTaskService.start("score", file.getOriginalFilename(), null);
        try {
            ImportResult<ScoreExcel> result = scoreService.importScores(file);
            importTaskService.complete(task.getId(), result.getTotal(), result.getSuccessCount(), result.getFailRows().size());
            return Result.success(result);
        } catch (RuntimeException ex) {
            importTaskService.fail(task.getId(), ex.getMessage());
            throw ex;
        }
    }

    @Operation(summary = "导出导入失败明细(Excel)")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','PRACTICE_TEACHER')")
    @PostMapping("/import-fail-export")
    public void exportImportFail(@RequestBody List<ScoreExcel> failRows, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("成绩导入失败明细", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), ScoreExcel.class).sheet("导入失败明细").doWrite(failRows);
    }
}
