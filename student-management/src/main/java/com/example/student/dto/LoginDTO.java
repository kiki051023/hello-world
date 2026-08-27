package com.example.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码 key（GET /api/auth/captcha 获取） */
    private String captchaKey;

    /** 用户输入的验证码 */
    private String captchaCode;
}
