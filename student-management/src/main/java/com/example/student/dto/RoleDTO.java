package com.example.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新增/编辑角色请求。
 */
@Data
public class RoleDTO {

    private Long id;

    @NotBlank(message = "角色编码不能为空")
    @Size(min = 2, max = 50, message = "角色编码长度需在2-50之间")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 100, message = "角色名称过长")
    private String roleName;

    @Size(max = 255, message = "描述过长")
    private String description;

    @NotNull(message = "状态不能为空")
    private Integer status;

    /** 关联的权限ID列表（分配权限用） */
    private List<Long> permissionIds;

    /** 角色级数据权限模板 */
    private List<UserDataScopeDTO> dataScopes;
}