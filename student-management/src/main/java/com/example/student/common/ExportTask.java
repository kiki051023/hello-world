package com.example.student.common;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 异步导出任务：超过同步阈值（ImportLimits.MAX_EXPORT_ROWS=10000）的导出转此表，
 * 由后台线程池逐个执行；前端凭 taskNo 轮询状态后下载。
 */
@Data
@TableName("export_task")
public class ExportTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskNo;

    /** SCORE/STUDENT/... 业务类型 */
    private String businessType;

    private String paramsJson;

    /** PENDING/RUNNING/SUCCESS/FAILED */
    private String status;

    private Integer totalRows;

    private Long requesterId;

    private String resultKey;

    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
