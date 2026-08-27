package com.example.student.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.Notification;

public interface NotificationService extends IService<Notification> {

    /** 当前用户通知分页（时间倒序，默认未读优先） */
    IPage<Notification> getNotificationPage(Long current, Long size, Long receiverId,
                                            Integer isRead);

    /** 当前用户未读计数（红点） */
    long countUnread(Long receiverId);

    /** 标记单条已读（校验归属） */
    void markRead(Long id, Long receiverId);

    /** 标记全部已读 */
    int markAllRead(Long receiverId);

    /** 发送通知（幂等去重：同 receiver+type+relatedId 不重复发未读） */
    void send(Long receiverId, String type, String title, String content, Long relatedId);
}