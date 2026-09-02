package com.example.student.service;

import com.example.student.entity.Notification;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.NotificationMapper;
import com.example.student.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** P2-D 通知中心：发送幂等、归属校验、已读标记 */
class NotificationServiceTest {

    private NotificationMapper mapper;
    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(NotificationMapper.class);
        service = new NotificationServiceImpl(mock(com.example.student.service.channel.NotifyChannelDispatcher.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        when(mapper.insert(any(Notification.class))).thenReturn(1);
        when(mapper.selectCount(any())).thenReturn(0L);
    }

    @Test
    void sendCreatesUnreadNotification() {
        service.send(77L, "LEAVE", "请假审批通过", "您的请假已通过", 100L);

        ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
        verify(mapper).insert(cap.capture());
        Notification n = cap.getValue();
        assertEquals(Long.valueOf(77L), n.getReceiverId());
        assertEquals("LEAVE", n.getType());
        assertEquals("请假审批通过", n.getTitle());
        assertEquals(Integer.valueOf(0), n.getIsRead());
        assertEquals(Long.valueOf(100L), n.getRelatedId());
    }

    @Test
    void sendSkipsNullReceiverOrBlankTitle() {
        service.send(null, "LEAVE", "标题", "内容", 1L);
        service.send(1L, "LEAVE", "", "内容", 1L);
        verify(mapper, never()).insert(any());
    }

    @Test
    void sendIsIdempotentForUnreadDuplicate() {
        when(mapper.selectCount(any())).thenReturn(1L);
        service.send(77L, "LEAVE", "请假审批通过", "您的请假已通过", 100L);
        verify(mapper, never()).insert(any());
    }

    @Test
    void markReadRejectsMissingOrForeignNotification() {
        when(mapper.selectById(1L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.markRead(1L, 77L));

        Notification n = new Notification();
        n.setId(2L);
        n.setReceiverId(99L);
        n.setIsRead(0);
        when(mapper.selectById(2L)).thenReturn(n);
        assertThrows(BusinessException.class, () -> service.markRead(2L, 77L));
    }

    @Test
    void markReadSkipsAlreadyRead() {
        Notification n = new Notification();
        n.setId(3L);
        n.setReceiverId(77L);
        n.setIsRead(1);
        when(mapper.selectById(3L)).thenReturn(n);

        service.markRead(3L, 77L);

        verify(mapper, never()).updateById(any());
    }

    @Test
    void markReadUpdatesUnreadToRead() {
        Notification n = new Notification();
        n.setId(4L);
        n.setReceiverId(77L);
        n.setIsRead(0);
        when(mapper.selectById(4L)).thenReturn(n);

        service.markRead(4L, 77L);

        assertEquals(Integer.valueOf(1), n.getIsRead());
        verify(mapper).updateById(n);
    }

    @Test
    void markAllReadDelegatesToMapper() {
        when(mapper.markAllRead(77L)).thenReturn(3);
        assertEquals(3, service.markAllRead(77L));
        verify(mapper).markAllRead(77L);
    }
}