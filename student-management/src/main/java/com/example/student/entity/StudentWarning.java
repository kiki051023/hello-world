package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("student_warning")
public class StudentWarning implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    /** SCORE/LEAVE/RP/FUNDING */
    private String warningType;

    private String ruleKey;

    private Integer ruleThreshold;

    private Integer currentValue;

    /** ACTIVE/HANDLED/DISMISSED */
    private String status;

    private Long handledBy;

    private String handleNote;

    private LocalDateTime handledAt;

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
