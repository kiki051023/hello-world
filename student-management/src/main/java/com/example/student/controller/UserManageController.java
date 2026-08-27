package com.example.student.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.common.PageQuery;
import com.example.student.common.PageResult;
import com.example.student.common.Result;
import com.example.student.dto.UserCreateDTO;
import com.example.student.dto.UserDataScopeDTO;
import com.example.student.dto.UserUpdateDTO;
import com.example.student.entity.Role;
import com.example.student.entity.User;
import com.example.student.security.RoleManageDataScopeGuard;
import com.example.student.service.UserManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理（仅 ADMIN）。
 * 所有操作记录审计（OperationAuditAspect 切 controller..*）。
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/system/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserManageController {

    private final UserManageService userManageService;
    private final RoleManageDataScopeGuard dataScopeGuard;

    public UserManageController(UserManageService userManageService, RoleManageDataScopeGuard dataScopeGuard) {
        this.userManageService = userManageService;
        this.dataScopeGuard = dataScopeGuard;
    }

    @Operation(summary = "用户分页")
    @GetMapping("/page")
    public Result<PageResult<User>> page(@Valid PageQuery pageQuery,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String role,
                                         @RequestParam(required = false) Integer status) {
        IPage<User> page = userManageService.pageUsers(
                pageQuery.getCurrent(), pageQuery.getSize(), keyword, role, status);
        return Result.success(PageResult.of(
                page.getTotal(), page.getRecords(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "用户详情（含角色）")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        User user = userManageService.getUserById(id);
        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("roles", userManageService.rolesOfUser(id));
        return Result.success(data);
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody UserCreateDTO dto) {
        dataScopeGuard.assertAdmin();
        userManageService.createUser(dto);
        return Result.success();
    }

    @Operation(summary = "修改用户基本信息")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        dto.setId(id);
        userManageService.updateUser(dto);
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @RequestParam String newPassword) {
        userManageService.resetPassword(id, newPassword);
        return Result.success();
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        userManageService.changeStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "调整用户角色")
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        dataScopeGuard.assertAdmin();
        userManageService.assignRoles(id, roleIds);
        return Result.success();
    }

    @Operation(summary = "调整用户数据权限")
    @PutMapping("/{id}/data-scope")
    public Result<Void> assignDataScopes(@PathVariable Long id,
                                         @RequestBody List<UserDataScopeDTO> scopes) {
        dataScopeGuard.assertAdmin();
        userManageService.assignDataScopes(id, scopes);
        return Result.success();
    }

    @Operation(summary = "用户已绑定角色")
    @GetMapping("/{id}/roles")
    public Result<List<Role>> rolesOfUser(@PathVariable Long id) {
        return Result.success(userManageService.rolesOfUser(id));
    }
}