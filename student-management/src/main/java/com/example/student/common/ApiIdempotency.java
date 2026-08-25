package com.example.student.common;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 幂等键登记：同一 Idempotency-Key 的重复写请求返回首次结果，不重复执行。 */
@Data
@TableName("api_idempotency")
public class ApiIdempotency implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户端提供的 Idempotency-Key */
    private String idempotencyKey;

    /** 请求方法+路径，防止不同端点撞键 */
    private String requestFingerprint;

    /** 首次执行的响应体（JSON），重复请求直接回放 */
    private String responseBody;

    private Integer httpStatus;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;
}
