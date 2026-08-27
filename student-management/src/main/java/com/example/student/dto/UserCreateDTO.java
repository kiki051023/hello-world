package com.example.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增用户请求。初始密码由管理员设置，后端强制强度校验。
 */
@Data
public class UserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度需在2-50之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字、下划线")
    private String username;

    @NotBlank(message = "初始密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度需在8-64之间")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "密码必须包含大写字母、小写字母和数字"
    )
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名过长")
    private String realName;

    @Size(max = 30, message = "角色编码过长")
    private String role;

    private Long schoolId;

    private Long departmentId;

    @Size(max = 30, message = "人员类型过长")
    private String employeeType;

    /** 关联的角色ID列表（可多选） */
    private java.util.List<Long> roleIds;

    /** 用户级数据权限（可选） */
    private java.util.List<com.example.student.dto.UserDataScopeDTO> dataScopes;
}