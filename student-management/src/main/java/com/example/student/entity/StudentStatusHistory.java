package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 学籍异动历史（只追加）。 */
@Data
@TableName("student_status_history")
public class StudentStatusHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    /** STATUS/TRANSFER_MAJOR/TRANSFER_CLASS/SUSPEND/RESUME/GRADUATE */
    private String changeType;

    private String oldValue;

    private String newValue;

    private String reason;

    private Long operatorId;

    private String operatorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
