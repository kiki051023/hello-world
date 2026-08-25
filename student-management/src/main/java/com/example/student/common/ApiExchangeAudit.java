package com.example.student.common;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 开放接口交换审计（只追加）。 */
@Data
@TableName("api_exchange_audit")
public class ApiExchangeAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String clientId;

    private String apiVersion;

    private String requestMethod;

    private String requestPath;

    private String idempotencyKey;

    private Integer httpStatus;

    private String rejectedReason;

    private String requestId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
