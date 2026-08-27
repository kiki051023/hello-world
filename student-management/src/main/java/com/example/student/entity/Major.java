package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 专业（phase1 建表，P2-C 补齐实体）
 */
@Data
@TableName("major")
public class Major implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属系部 */
    private Long departmentId;

    /** 专业编码（系内唯一，uk_major_department_code 约束为 department_id+major_code） */
    private String majorCode;

    /** 专业名称 */
    private String majorName;

    /** 学历层次（如：大专/本科） */
    private String educationLevel;

    /** 学制（年） */
    private BigDecimal durationYears;

    /** 状态：1-启用 0-停用 */
    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 冗余展示：所属系部名称 */
    @TableField(exist = false)
    private String departmentName;
}