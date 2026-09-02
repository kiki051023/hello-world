package com.example.student.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.common.PageQuery;
import com.example.student.common.PageResult;
import com.example.student.common.Result;
import com.example.student.entity.DormRoom;
import com.example.student.service.DormRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** 宿舍管理（代码生成） */
@Tag(name = "宿舍管理")
@RestController
@RequestMapping("/api/dorm")
public class DormRoomController {

    @Autowired
    private DormRoomService dormRoomService;

    @Operation(summary = "分页")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/page")
    public Result<PageResult<DormRoom>> page(@Valid @ModelAttribute PageQuery q,
                                       @RequestParam(required = false) String keyword) {
        IPage<DormRoom> page = dormRoomService.getPage(q.getCurrent(), q.getSize(), keyword);
        return Result.success(PageResult.of(page.getTotal(), page.getRecords(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "新增")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Result<Void> add(@RequestBody DormRoom body) {
        dormRoomService.save(body);
        return Result.success();
    }

    @Operation(summary = "修改")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public Result<Void> update(@RequestBody DormRoom body) {
        dormRoomService.updateById(body);
        return Result.success();
    }

    @Operation(summary = "删除")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dormRoomService.removeById(id);
        return Result.success();
    }
}
