package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("training_plan_course")
public class TrainingPlanCourse implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private Long courseId;

    /** REQUIRED 必修 / ELECTIVE 选修 */
    private String courseType;

    private Integer suggestTermNo;

    private BigDecimal credit;

    @TableField(exist = false)
    private String courseName;

    @TableField(exist = false)
    private String courseNo;
}
