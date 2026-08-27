package com.example.student.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.common.PageQuery;
import com.example.student.common.PageResult;
import com.example.student.common.Result;
import com.example.student.dto.RoleDTO;
import com.example.student.dto.UserDataScopeDTO;
import com.example.student.entity.DataScope;
import com.example.student.entity.Permission;
import com.example.student.entity.Role;
import com.example.student.security.RoleManageDataScopeGuard;
import com.example.student.service.RoleManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色管理（仅 ADMIN）。
 * 内置角色（8 个固定编码）锁定编码与状态、禁止删除。
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/system/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleManageController {

    private final RoleManageService roleManageService;
    private final RoleManageDataScopeGuard dataScopeGuard;

    public RoleManageController(RoleManageService roleManageService, RoleManageDataScopeGuard dataScopeGuard) {
        this.roleManageService = roleManageService;
        this.dataScopeGuard = dataScopeGuard;
    }

    @Operation(summary = "角色分页")
    @GetMapping("/page")
    public Result<PageResult<Role>> page(@Valid PageQuery pageQuery,
                                         @RequestParam(required = false) String keyword) {
        IPage<Role> page = roleManageService.pageRoles(pageQuery.getCurrent(), pageQuery.getSize(), keyword);
        return Result.success(PageResult.of(page.getTotal(), page.getRecords(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "全部启用角色（下拉用）")
    @GetMapping("/list")
    public Result<List<Role>> list() {
        return Result.success(roleManageService.listAll());
    }

    @Operation(summary = "全部权限字典（分配用）")
    @GetMapping("/permissions")
    public Result<List<Permission>> permissions() {
        return Result.success(roleManageService.listAllPermissions());
    }

    @Operation(summary = "角色详情（含权限与数据范围）")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Map<String, Object> data = new HashMap<>();
        data.put("role", roleManageService.getRoleById(id));
        data.put("permissions", roleManageService.permissionsOfRole(id));
        data.put("dataScopes", roleManageService.dataScopesOfRole(id));
        return Result.success(data);
    }

    @Operation(summary = "新建角色")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody RoleDTO dto) {
        roleManageService.createRole(dto);
        return Result.success();
    }

    @Operation(summary = "编辑角色")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleDTO dto) {
        dto.setId(id);
        roleManageService.updateRole(dto);
        return Result.success();
    }

    @Operation(summary = "删除角色（内置角色禁止）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleManageService.deleteRole(id);
        return Result.success();
    }

    @Operation(summary = "角色已拥权限")
    @GetMapping("/{id}/permissions")
    public Result<List<Permission>> permissionsOfRole(@PathVariable Long id) {
        return Result.success(roleManageService.permissionsOfRole(id));
    }

    @Operation(summary = "分配角色权限")
    @PutMapping("/{id}/permissions")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        dataScopeGuard.assertAdmin();
        roleManageService.assignPermissions(id, permissionIds);
        return Result.success();
    }

    @Operation(summary = "角色数据范围模板")
    @GetMapping("/{id}/data-scope")
    public Result<List<DataScope>> dataScopesOfRole(@PathVariable Long id) {
        return Result.success(roleManageService.dataScopesOfRole(id));
    }

    @Operation(summary = "分配角色数据范围模板")
    @PutMapping("/{id}/data-scope")
    public Result<Void> assignDataScopes(@PathVariable Long id, @RequestBody List<UserDataScopeDTO> scopes) {
        dataScopeGuard.assertAdmin();
        roleManageService.assignDataScopes(id, scopes);
        return Result.success();
    }
}