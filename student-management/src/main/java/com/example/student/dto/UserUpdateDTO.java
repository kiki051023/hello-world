package com.example.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改用户基本信息。密码不在此处修改（走独立的 reset-password 接口）。
 */
@Data
public class UserUpdateDTO {

    private Long id;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名过长")
    private String realName;

    @Size(max = 30, message = "人员类型过长")
    private String employeeType;

    private Long schoolId;

    private Long departmentId;
}