package com.example.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 强制改密请求（首次登录/管理员重置密码后）：不校验旧密码（用户未知），
 * 但要求当前用户 mustChangePassword=1（后端校验，否则拒绝）。
 */
@Data
public class ForceChangePasswordDTO {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "新密码长度需在8-64之间")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "新密码必须包含大写字母、小写字母和数字"
    )
    private String newPassword;
}