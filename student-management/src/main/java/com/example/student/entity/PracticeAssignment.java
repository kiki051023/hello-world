package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("practice_assignment")
public class PracticeAssignment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private Long studentId;

    private Long companyId;

    private Long positionId;

    private Long supervisorUserId;

    /** 校内指导教师ID(teacher.id)——数据范围锚点 */
    private Long instructorTeacherId;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal practiceScore;

    /** ACTIVE/FINISHED/CANCELLED */
    private String status;

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

    @TableField(exist = false)
    private String companyName;

    @TableField(exist = false)
    private String positionName;

    @TableField(exist = false)
    private String instructorName;
}
