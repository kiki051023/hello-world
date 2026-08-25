package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 只追加的成绩变更历史，禁止修改和删除。 */
@Data
@TableName("score_change_log")
public class ScoreChangeLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long scoreId;

    /** CREATE/SUBMIT/APPROVE/REJECT/VOID/CHANGE/MAKEUP/RETAKE */
    private String changeType;

    private BigDecimal oldScore;

    private BigDecimal newScore;

    private String reason;

    private Long operatorId;

    private String operatorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
