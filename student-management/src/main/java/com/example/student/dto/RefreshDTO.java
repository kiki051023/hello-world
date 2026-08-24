package com.example.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshDTO {

    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}
