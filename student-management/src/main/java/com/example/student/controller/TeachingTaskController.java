package com.example.student.controller;

import com.example.student.common.PageQuery;
import com.example.student.common.Result;
import com.example.student.entity.TeachingTask;
import com.example.student.service.TeachingTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "授课任务")
@RestController
@RequestMapping("/api/teaching-task")
public class TeachingTaskController {

    @Autowired
    private TeachingTaskService teachingTaskService;

    @Operation(summary = "教学任务列表（分页，教师仅见本人）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER')")
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@Valid @ModelAttribute PageQuery pageQuery,
                                            @RequestParam(required = false) Long teacherId,
                                            @RequestParam(required = false) Long termId,
                                            @RequestParam(required = false) Long clazzId) {
        return Result.success(teachingTaskService.getPage(pageQuery.getCurrent(), pageQuery.getSize(),
                teacherId, termId, clazzId));
    }

    @Operation(summary = "教学任务详情")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER')")
    @GetMapping("/{id}")
    public Result<TeachingTask> getById(@PathVariable Long id) {
        return Result.success(teachingTaskService.getByIdChecked(id));
    }

    @Operation(summary = "新增教学任务")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER')")
    @PostMapping
    public Result<Void> add(@RequestBody TeachingTask task) {
        teachingTaskService.addTeachingTask(task);
        return Result.success();
    }

    @Operation(summary = "修改教学任务")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER')")
    @PutMapping
    public Result<Void> update(@RequestBody TeachingTask task) {
        teachingTaskService.updateTeachingTask(task);
        return Result.success();
    }

    @Operation(summary = "删除教学任务")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        teachingTaskService.deleteTeachingTask(id);
        return Result.success();
    }
}
