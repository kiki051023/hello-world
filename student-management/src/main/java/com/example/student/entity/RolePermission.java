package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色-权限关联（sys_role_permission，复合主键 role_id + permission_id）。
 */
@Data
@TableName("sys_role_permission")
public class RolePermission implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long roleId;

    private Long permissionId;

    private LocalDateTime createTime;
}