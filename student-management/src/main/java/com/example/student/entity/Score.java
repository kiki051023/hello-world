package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("score")
public class Score implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long courseId;

    private BigDecimal score;

    private LocalDate examTime;

    /** 开课ID，0 为历史数据未关联 */
    private Long courseTermId;

    /** DRAFT/SUBMITTED/APPROVED/REJECTED/VOID */
    private String scoreStatus;

    /** REGULAR/MAKEUP/RETAKE */
    private String scoreType;

    /** 当前有效版本标志：仅 APPROVED 记录为 1 */
    private Integer active;

    /** 补考/重修关联的原成绩ID */
    private Long originalScoreId;

    /** 业务版本号 */
    private Integer versionNo;

    /** 乐观锁版本 */
    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String studentName;

    @TableField(exist = false)
    private String studentNo;

    @TableField(exist = false)
    private String courseName;
}
