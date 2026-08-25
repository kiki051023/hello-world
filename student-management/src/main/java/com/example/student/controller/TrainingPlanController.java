package com.example.student.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.common.PageQuery;
import com.example.student.common.Result;
import com.example.student.entity.TrainingPlan;
import com.example.student.entity.TrainingPlanCourse;
import com.example.student.service.TrainingPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "培养方案")
@RestController
@RequestMapping("/api/training-plan")
public class TrainingPlanController {

    @Autowired
    private TrainingPlanService trainingPlanService;

    @Operation(summary = "培养方案列表（分页）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','STUDENT')")
    @GetMapping("/page")
    public Result<IPage<TrainingPlan>> page(@Valid @ModelAttribute PageQuery pageQuery,
                                            @RequestParam(required = false) Long majorId,
                                            @RequestParam(required = false) Integer enrollmentYear) {
        return Result.success(trainingPlanService.getPage(pageQuery.getCurrent(), pageQuery.getSize(),
                majorId, enrollmentYear));
    }

    @Operation(summary = "培养方案详情")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','STUDENT')")
    @GetMapping("/{id}")
    public Result<TrainingPlan> getById(@PathVariable Long id) {
        return Result.success(trainingPlanService.getDetail(id));
    }

    @Operation(summary = "培养方案课程列表")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','STUDENT')")
    @GetMapping("/{id}/courses")
    public Result<List<Map<String, Object>>> courses(@PathVariable Long id) {
        return Result.success(trainingPlanService.listCourses(id));
    }

    @Operation(summary = "新增培养方案")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER')")
    @PostMapping
    public Result<Void> add(@RequestBody TrainingPlan plan) {
        trainingPlanService.addPlan(plan);
        return Result.success();
    }

    @Operation(summary = "修改培养方案")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER')")
    @PutMapping
    public Result<Void> update(@RequestBody TrainingPlan plan) {
        trainingPlanService.updatePlan(plan);
        return Result.success();
    }

    @Operation(summary = "删除培养方案")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        trainingPlanService.deletePlan(id);
        return Result.success();
    }

    @Operation(summary = "保存培养方案课程（全量替换）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER')")
    @PutMapping("/{id}/courses")
    public Result<Void> saveCourses(@PathVariable Long id, @RequestBody List<TrainingPlanCourse> courses) {
        trainingPlanService.saveCourses(id, courses);
        return Result.success();
    }
}
