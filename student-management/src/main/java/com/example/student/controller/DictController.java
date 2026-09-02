package com.example.student.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.common.PageQuery;
import com.example.student.common.PageResult;
import com.example.student.common.Result;
import com.example.student.entity.DictData;
import com.example.student.entity.DictType;
import com.example.student.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据字典（M2-1）：管理端 CRUD（ADMIN）+ 公开查询（任何登录用户拉取下拉/翻译）。
 */
@Tag(name = "数据字典")
@RestController
@RequestMapping("/api/system/dict")
public class DictController {

    @Autowired
    private DictService dictService;

    // ---------- 公开查询（登录即可，前端下拉/标签翻译用） ----------

    @Operation(summary = "某类型下启用字典数据（下拉源）")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/data/list/{dictType}")
    public Result<List<DictData>> listEnabledData(@PathVariable String dictType) {
        return Result.success(dictService.listEnabledData(dictType));
    }

    // ---------- 类型管理（ADMIN） ----------

    @Operation(summary = "字典类型分页")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/type/page")
    public Result<PageResult<DictType>> typePage(@Valid @ModelAttribute PageQuery pageQuery,
                                                 @RequestParam(required = false) String keyword) {
        IPage<DictType> page = dictService.getTypePage(pageQuery.getCurrent(), pageQuery.getSize(), keyword);
        return Result.success(PageResult.of(page.getTotal(), page.getRecords(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "新增字典类型")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/type")
    public Result<Void> addType(@RequestBody DictType type) {
        dictService.addType(type);
        return Result.success();
    }

    @Operation(summary = "修改字典类型（dictType 不可改）")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/type")
    public Result<Void> updateType(@RequestBody DictType type) {
        dictService.updateType(type);
        return Result.success();
    }

    @Operation(summary = "删除字典类型（下有数据拒绝）")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/type/{id}")
    public Result<Void> deleteType(@PathVariable Long id) {
        dictService.deleteType(id);
        return Result.success();
    }

    // ---------- 数据管理（ADMIN） ----------

    @Operation(summary = "字典数据分页")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/data/page")
    public Result<PageResult<DictData>> dataPage(@Valid @ModelAttribute PageQuery pageQuery,
                                                 @RequestParam String dictType) {
        IPage<DictData> page = dictService.getDataPage(pageQuery.getCurrent(), pageQuery.getSize(), dictType);
        return Result.success(PageResult.of(page.getTotal(), page.getRecords(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "新增字典数据")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/data")
    public Result<Void> addData(@RequestBody DictData data) {
        dictService.addData(data);
        return Result.success();
    }

    @Operation(summary = "修改字典数据")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/data")
    public Result<Void> updateData(@RequestBody DictData data) {
        dictService.updateData(data);
        return Result.success();
    }

    @Operation(summary = "删除字典数据")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/data/{id}")
    public Result<Void> deleteData(@PathVariable Long id) {
        dictService.deleteData(id);
        return Result.success();
    }
}
