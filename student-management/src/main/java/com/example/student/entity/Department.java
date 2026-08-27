package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系部/二级学院（phase1 建表，P2-C 补齐实体）
 */
@Data
@TableName("department")
public class Department implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属学校 */
    private Long schoolId;

    /** 系部编码（校内唯一，uk_school_code 约束为 school_id+department_code） */
    private String departmentCode;

    /** 系部名称 */
    private String departmentName;

    /** 负责人用户 ID */
    private Long leaderUserId;

    /** 状态：1-启用 0-停用 */
    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 冗余展示：所属学校名称（列表页便捷展示） */
    @TableField(exist = false)
    private String schoolName;
}