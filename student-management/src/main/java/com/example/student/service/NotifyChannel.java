package com.example.student.service;

import com.example.student.entity.Notification;

/**
 * 通知外发渠道（M3-1）：站内信之外的可选通道。
 * 实现：邮件（用户 email 非空才发）、企业微信群 webhook（配置非空才发）。
 * 每个实现自行决定 supports()；失败记 sys_notify_channel_log 由调度重试。
 */
public interface NotifyChannel {

    /** 渠道标识：EMAIL / WECOM */
    String channel();

    /** 该次发送是否适用（如用户无邮箱则邮件渠道返回 false） */
    boolean supports(Long receiverId);

    /** 执行发送，抛异常即失败（由调度器捕获记日志） */
    void deliver(Long receiverId, Notification notification) throws Exception;
}
