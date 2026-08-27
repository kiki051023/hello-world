package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据权限范围（sys_data_scope）：
 * - user_id 非空：用户级数据权限
 * - role_id 非空：角色级数据权限模板（该角色用户继承）
 * - scopeType: SCHOOL / DEPARTMENT / MAJOR / CLAZZ / COURSE / SELF / ALL
 * - 也作为 RoleMapper 查询的 resultType（按别名映射，未查字段为 null）
 */
@Data
@TableName("sys_data_scope")
public class DataScope implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long roleId;

    private String scopeType;

    private Long scopeValue;

    private LocalDateTime createTime;
}