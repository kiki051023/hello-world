package com.example.student.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.common.ImportResult;
import com.example.student.common.PageResult;
import com.example.student.common.Result;
import com.example.student.entity.Clazz;
import com.example.student.excel.ClazzExcel;
import com.example.student.exception.BusinessException;
import com.example.student.service.ClazzService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "班级管理")
@RestController
@RequestMapping("/api/clazz")
public class ClazzController {

    @Autowired
    private ClazzService clazzService;

    @Operation(summary = "班级列表（分页）")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
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
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    @GetMapping("/list")
    public Result<List<Clazz>> getAllClazz() {
        return Result.success(clazzService.getAllClazz());
    }

    @Operation(summary = "班级详情")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    @GetMapping("/{id}")
    public Result<Clazz> getClazzById(@PathVariable Long id) {
        return Result.success(clazzService.getById(id));
    }

    @Operation(summary = "新增班级")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Result<Void> addClazz(@RequestBody Clazz clazz) {
        clazzService.addClazz(clazz);
        return Result.success();
    }

    @Operation(summary = "修改班级")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public Result<Void> updateClazz(@RequestBody Clazz clazz) {
        clazzService.updateClazz(clazz);
        return Result.success();
    }

    @Operation(summary = "删除班级")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> deleteClazz(@PathVariable Long id) {
        clazzService.deleteClazz(id);
        return Result.success();
    }

    @Operation(summary = "导出班级(Excel)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/export")
    public void exportClazzs(HttpServletResponse response,
                             @RequestParam(required = false) String clazzName,
                             @RequestParam(required = false) String grade) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("班级信息", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), ClazzExcel.class)
                .sheet("班级信息").doWrite(clazzService.exportClazzs(clazzName, grade));
    }

    @Operation(summary = "导入班级(Excel)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import")
    public Result<ImportResult<ClazzExcel>> importClazzs(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        return Result.success(clazzService.importClazzs(file));
    }

    @Operation(summary = "导出导入失败明细(Excel)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import-fail-export")
    public void exportImportFail(@RequestBody List<ClazzExcel> failRows, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("班级导入失败明细", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), ClazzExcel.class).sheet("导入失败明细").doWrite(failRows);
    }
}
