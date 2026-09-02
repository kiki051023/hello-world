package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.Notification;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.NotificationMapper;
import com.example.student.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification>
        implements NotificationService {

    private final com.example.student.service.channel.NotifyChannelDispatcher channelDispatcher;

    public NotificationServiceImpl(com.example.student.service.channel.NotifyChannelDispatcher channelDispatcher) {
        this.channelDispatcher = channelDispatcher;
    }

    @Override
    public IPage<Notification> getNotificationPage(Long current, Long size, Long receiverId,
                                                   Integer isRead) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, receiverId)
                .eq(isRead != null, Notification::getIsRead, isRead)
                .orderByDesc(Notification::getCreateTime)
                .orderByDesc(Notification::getId);
        return page(new Page<>(current, size), wrapper);
    }

    @Override
    public long countUnread(Long receiverId) {
        return count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, receiverId)
                .eq(Notification::getIsRead, 0));
    }

    @Override
    @Transactional
    public void markRead(Long id, Long receiverId) {
        Notification notification = getById(id);
        if (notification == null || !notification.getReceiverId().equals(receiverId)) {
            throw new BusinessException("通知不存在");
        }
        if (notification.getIsRead() != null && notification.getIsRead() == 1) return;
        notification.setIsRead(1);
        updateById(notification);
    }

    @Override
    @Transactional
    public int markAllRead(Long receiverId) {
        return baseMapper.markAllRead(receiverId);
    }

    @Override
    @Transactional
    public void send(Long receiverId, String type, String title, String content, Long relatedId) {
        if (receiverId == null || !StringUtils.hasText(title)) return;
        // 幂等：同 receiver+type+relatedId 已有未读通知则不重复发
        Long exists = count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, receiverId)
                .eq(Notification::getType, type)
                .eq(relatedId != null, Notification::getRelatedId, relatedId)
                .eq(Notification::getIsRead, 0));
        if (exists != null && exists > 0) return;
        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setType(type == null ? "SYSTEM" : type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(relatedId);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        save(notification);
        // M3-1：站内信落库后 fan-out 外发渠道（邮件/企微），失败记日志待重试
        channelDispatcher.dispatch(notification);
    }
}