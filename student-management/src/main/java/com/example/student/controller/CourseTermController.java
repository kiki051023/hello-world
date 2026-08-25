package com.example.student.controller;

import com.example.student.common.PageQuery;
import com.example.student.common.Result;
import com.example.student.entity.CourseTerm;
import com.example.student.service.CourseTermService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "课程开设")
@RestController
@RequestMapping("/api/course-term")
public class CourseTermController {

    @Autowired
    private CourseTermService courseTermService;

    @Operation(summary = "开课列表（分页）")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','STUDENT')")
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@Valid @ModelAttribute PageQuery pageQuery,
                                            @RequestParam(required = false) Long courseId,
                                            @RequestParam(required = false) Long termId) {
        return Result.success(courseTermService.getPage(pageQuery.getCurrent(), pageQuery.getSize(), courseId, termId));
    }

    @Operation(summary = "开课详情")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER','COUNSELOR','TEACHER','STUDENT')")
    @GetMapping("/{id}")
    public Result<CourseTerm> getById(@PathVariable Long id) {
        return Result.success(courseTermService.getByIdChecked(id));
    }

    @Operation(summary = "新增开课")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER')")
    @PostMapping
    public Result<Void> add(@RequestBody CourseTerm courseTerm) {
        courseTermService.addCourseTerm(courseTerm);
        return Result.success();
    }

    @Operation(summary = "修改开课")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN','DEPARTMENT_LEADER')")
    @PutMapping
    public Result<Void> update(@RequestBody CourseTerm courseTerm) {
        courseTermService.updateCourseTerm(courseTerm);
        return Result.success();
    }

    @Operation(summary = "删除开课")
    @PreAuthorize("hasAnyRole('ADMIN','EDUCATION_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        courseTermService.deleteCourseTerm(id);
        return Result.success();
    }
}
