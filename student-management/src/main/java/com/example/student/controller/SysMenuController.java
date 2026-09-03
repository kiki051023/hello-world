package com.example.student.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.common.PageQuery;
import com.example.student.common.PageResult;
import com.example.student.common.Result;
import com.example.student.entity.SysMenu;
import com.example.student.security.AuthorizationService;
import com.example.student.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 动态菜单（M4-6）：登录用户拉自己角色的菜单树；ADMIN 维护。
 */
@Tag(name = "动态菜单")
@RestController
@RequestMapping("/api/system/menu")
public class SysMenuController {

    @Autowired
    private SysMenuService sysMenuService;

    @Autowired
    private AuthorizationService authorizationService;

    @Operation(summary = "当前角色可见菜单")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my")
    public Result<List<SysMenu>> my() {
        String role = authorizationService.currentUser().getRole();
        // role 存的是 ROLE_X 或裸码，统一去前缀
        if (role != null && role.startsWith("ROLE_")) role = role.substring(5);
        return Result.success(sysMenuService.listVisibleByRole(role));
    }

    @Operation(summary = "菜单分页（管理）")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/page")
    public Result<PageResult<SysMenu>> page(@Valid @ModelAttribute PageQuery pageQuery) {
        IPage<SysMenu> page = sysMenuService.getPage(pageQuery.getCurrent(), pageQuery.getSize());
        return Result.success(PageResult.of(page.getTotal(), page.getRecords(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "新增菜单")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Result<Void> add(@RequestBody SysMenu menu) {
        sysMenuService.save(menu);
        sysMenuService.evictAll();
        return Result.success();
    }

    @Operation(summary = "修改菜单")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public Result<Void> update(@RequestBody SysMenu menu) {
        sysMenuService.updateById(menu);
        sysMenuService.evictAll();
        return Result.success();
    }

    @Operation(summary = "删除菜单")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysMenuService.removeById(id);
        sysMenuService.evictAll();
        return Result.success();
    }
}
