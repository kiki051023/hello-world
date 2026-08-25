package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("teaching_task")
public class TeachingTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseTermId;

    private Long teacherId;

    private Long clazzId;

    private String taskRole;

    private BigDecimal weeklyHours;

    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String teacherName;

    @TableField(exist = false)
    private String clazzName;

    @TableField(exist = false)
    private String courseName;

    @TableField(exist = false)
    private String termName;
}
