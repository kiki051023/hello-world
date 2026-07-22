package com.example.student.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.common.PageResult;
import com.example.student.common.Result;
import com.example.student.entity.Clazz;
import com.example.student.service.ClazzService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "班级管理")
@RestController
@RequestMapping("/api/clazz")
public class ClazzController {

    @Autowired
    private ClazzService clazzService;

    @Operation(summary = "班级列表（分页）")
    @GetMapping("/page")
    public Result<PageResult<Clazz>> getClazzPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String clazzName,
            @RequestParam(required = false) String grade) {
        IPage<Clazz> page = clazzService.getClazzPage(current, size, clazzName, grade);
        return Result.success(PageResult.of(page.getTotal(), page.getRecords(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "所有班级列表")
    @GetMapping("/list")
    public Result<List<Clazz>> getAllClazz() {
        return Result.success(clazzService.getAllClazz());
    }

    @Operation(summary = "班级详情")
    @GetMapping("/{id}")
    public Result<Clazz> getClazzById(@PathVariable Long id) {
        return Result.success(clazzService.getById(id));
    }

    @Operation(summary = "新增班级")
    @PostMapping
    public Result<Void> addClazz(@RequestBody Clazz clazz) {
        clazzService.addClazz(clazz);
        return Result.success();
    }

    @Operation(summary = "修改班级")
    @PutMapping
    public Result<Void> updateClazz(@RequestBody Clazz clazz) {
        clazzService.updateClazz(clazz);
        return Result.success();
    }

    @Operation(summary = "删除班级")
    @DeleteMapping("/{id}")
    public Result<Void> deleteClazz(@PathVariable Long id) {
        clazzService.deleteClazz(id);
        return Result.success();
    }
}
