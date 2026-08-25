package com.example.student.audit;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long operatorId;
    private String username;
    private String action;
    private String requestMethod;
    private String requestPath;
    private String target;
    private String result;
    private String errorMessage;
    private String requestId;
    private String clientIp;
    private LocalDateTime createTime;
}
