package com.example.student.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.student.common.PageQuery;
import com.example.student.common.Result;
import com.example.student.audit.OperationLog;
import com.example.student.entity.ImportTask;
import com.example.student.mapper.ImportTaskMapper;
import com.example.student.mapper.OperationLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "系统接口")
@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private ImportTaskMapper importTaskMapper;

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("message", "学生管理系统运行正常");
        return Result.success(data);
    }

    @Operation(summary = "审计日志分页（管理员）")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/operation-logs")
    public Result<Map<String, Object>> operationLogs(@Valid PageQuery pageQuery,
                                                     @RequestParam(required = false) String result) {
        LambdaQueryWrapper<OperationLog> w = new LambdaQueryWrapper<OperationLog>()
                .eq(result != null && !result.isBlank(), OperationLog::getResult, result)
                .orderByDesc(OperationLog::getId);
        IPage<OperationLog> page = operationLogMapper.selectPage(
                new Page<>(pageQuery.getCurrent(), pageQuery.getSize()), w);
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        return Result.success(data);
    }

    @Operation(summary = "导入任务列表（管理员）")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/import-tasks")
    public Result<Map<String, Object>> importTasks(@Valid PageQuery pageQuery,
                                                   @RequestParam(required = false) String status) {
        LambdaQueryWrapper<ImportTask> w = new LambdaQueryWrapper<ImportTask>()
                .eq(status != null && !status.isBlank(), ImportTask::getStatus, status)
                .orderByDesc(ImportTask::getId);
        IPage<ImportTask> page = importTaskMapper.selectPage(
                new Page<>(pageQuery.getCurrent(), pageQuery.getSize()), w);
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        return Result.success(data);
    }
}
