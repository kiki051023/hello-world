package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户-角色关联（sys_user_role，复合主键 user_id + role_id）。
 * 仅用于关联写/删操作；查询走 RoleMapper XML。
 */
@Data
@TableName("sys_user_role")
public class UserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long roleId;

    private LocalDateTime createTime;
}