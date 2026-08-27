package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String courseNo;

    private String courseName;

    private Integer credit;

    private Long teacherId;

    /** 历史遗留字段：course 表无 school_id 列，标注非表字段防止 MyBatis-Plus 全字段 SELECT 报 Unknown column */
    @TableField(exist = false)
    private Long schoolId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String teacherName;
}
