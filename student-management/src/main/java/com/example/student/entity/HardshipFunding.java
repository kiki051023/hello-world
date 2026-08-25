package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 敏感数据表：sensitive_note 仅授权角色可见，列表接口默认脱敏。 */
@Data
@TableName("hardship_funding")
public class HardshipFunding implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private String academicYear;

    /** GRANT/SCHOLARSHIP/LOAN/WAIVER */
    private String fundingType;

    private BigDecimal amount;

    private String difficultyLevel;

    /** PENDING/APPROVED/REJECTED/DISBURSED */
    private String status;

    private Long reviewerId;

    private String reviewComment;

    /** 敏感字段：仅 ADMIN/EDUCATION_ADMIN/COUNSELOR(本班) 可见 */
    private String sensitiveNote;

    private Long operatorId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String studentNo;

    @TableField(exist = false)
    private String studentName;
}
