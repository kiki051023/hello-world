package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 定时任务执行日志（M1-1）：每次执行一行，成功/失败均记录 */
@Data
@TableName("sys_scheduled_task_log")
public class ScheduledTaskLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskCode;

    private String taskName;

    /** SUCCESS/FAILED */
    private String status;

    private String errorMessage;

    private Long costMs;

    private LocalDateTime createTime;
}