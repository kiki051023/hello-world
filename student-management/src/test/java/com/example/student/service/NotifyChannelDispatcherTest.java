package com.example.student.service;

import com.example.student.entity.Notification;
import com.example.student.entity.NotifyChannelLog;
import com.example.student.mapper.NotifyChannelLogMapper;
import com.example.student.service.channel.NotifyChannelDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** M3-1 渠道分发：supports 过滤、成功/失败日志、单渠道失败不影响其他 */
class NotifyChannelDispatcherTest {

    private NotifyChannelLogMapper logMapper;
    private NotifyChannel ok;
    private NotifyChannel bad;
    private NotifyChannel off;
    private NotifyChannelDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        logMapper = mock(NotifyChannelLogMapper.class);
        ok = mock(NotifyChannel.class);
        bad = mock(NotifyChannel.class);
        off = mock(NotifyChannel.class);
        when(ok.channel()).thenReturn("EMAIL");
        when(bad.channel()).thenReturn("WECOM");
        when(off.channel()).thenReturn("SMS");
        when(ok.supports(any())).thenReturn(true);
        when(bad.supports(any())).thenReturn(true);
        when(off.supports(any())).thenReturn(false);
        dispatcher = new NotifyChannelDispatcher(List.of(ok, bad, off), logMapper);
    }

    private Notification notif() {
        Notification n = new Notification();
        n.setId(1L);
        n.setReceiverId(7L);
        n.setType("LEAVE");
        n.setTitle("t");
        n.setContent("c");
        return n;
    }

    @Test
    void dispatchSkipsUnsupportedAndLogsEachAttempt() throws Exception {
        doThrow(new RuntimeException("smtp down")).when(bad).deliver(any(), any());

        dispatcher.dispatch(notif());

        verify(ok).deliver(eq(7L), any());
        verify(bad).deliver(eq(7L), any());
        verify(off, never()).deliver(any(), any());

        ArgumentCaptor<NotifyChannelLog> captor = ArgumentCaptor.forClass(NotifyChannelLog.class);
        verify(logMapper, times(2)).insert(captor.capture());
        List<NotifyChannelLog> logs = captor.getAllValues();
        assertEquals("SUCCESS", logs.get(0).getStatus());
        assertEquals("FAILED", logs.get(1).getStatus());
        assertEquals("smtp down", logs.get(1).getError());
    }
}
