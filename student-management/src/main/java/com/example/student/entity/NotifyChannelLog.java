package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 通知外发渠道日志（M3-1）：EMAIL/WECOM 发送结果 + 重试 */
@Data
@TableName("sys_notify_channel_log")
public class NotifyChannelLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long notificationId;
    private Long receiverId;

    /** EMAIL / WECOM */
    private String channel;

    private String target;

    /** PENDING / SUCCESS / FAILED */
    private String status;

    private String error;

    private Integer retryCount;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
