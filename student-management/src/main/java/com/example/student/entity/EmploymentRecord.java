package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("employment_record")
public class EmploymentRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Integer graduationYear;

    /** EMPLOYED/FURTHER_STUDY/ENTREPRENEUR/MILITARY/UNEMPLOYED/FLEXIBLE */
    private String destinationType;

    private String employerName;

    private String positionTitle;

    private String city;

    private LocalDate contractStart;

    private Integer confirmed;

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
