package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("import_task")
public class ImportTask {
    @TableId
    private Long id;
    private String taskNo;
    private String businessType;
    private String fileName;
    private String status;
    private Integer totalCount;
    private Integer successCount;
    private Integer failureCount;
    private String errorMessage;
    private Long operatorId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
