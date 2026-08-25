package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role")
public class Role {
    @TableId
    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer status;
    private Integer deleted;
}
