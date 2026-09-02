package com.example.student.service.channel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.student.entity.Notification;
import com.example.student.entity.NotifyChannelLog;
import com.example.student.mapper.NotifyChannelLogMapper;
import com.example.student.service.NotifyChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 渠道分发器（M3-1）：站内信落库后 fan-out 到所有 supports 的渠道。
 * 每个渠道一次发送写一条 sys_notify_channel_log；失败记 FAILED 待调度重试。
 */
@Component
public class NotifyChannelDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotifyChannelDispatcher.class);

    private final List<NotifyChannel> channels;
    private final NotifyChannelLogMapper logMapper;

    public NotifyChannelDispatcher(List<NotifyChannel> channels, NotifyChannelLogMapper logMapper) {
        this.channels = channels;
        this.logMapper = logMapper;
    }

    /** 分发一条站内通知到所有适用渠道（同步、容错：单渠道失败不影响其他） */
    public void dispatch(Notification n) {
        for (NotifyChannel ch : channels) {
            if (!ch.supports(n.getReceiverId())) continue;
            NotifyChannelLog entry = new NotifyChannelLog();
            entry.setNotificationId(n.getId());
            entry.setReceiverId(n.getReceiverId());
            entry.setChannel(ch.channel());
            entry.setRetryCount(0);
            try {
                ch.deliver(n.getReceiverId(), n);
                entry.setStatus("SUCCESS");
            } catch (Exception e) {
                entry.setStatus("FAILED");
                entry.setError(truncate(e.getMessage()));
                log.warn("notify channel {} failed for userId={}: {}", ch.channel(), n.getReceiverId(), e.getMessage());
            }
            logMapper.insert(entry);
        }
    }

    /** 重试 FAILED 且次数 < maxRetry 的记录，返回本轮重试条数 */
    public int retryFailed(int maxRetry) {
        List<NotifyChannelLog> failed = logMapper.selectList(new LambdaQueryWrapper<NotifyChannelLog>()
                .eq(NotifyChannelLog::getStatus, "FAILED")
                .lt(NotifyChannelLog::getRetryCount, maxRetry)
                .last("LIMIT 50"));
        int retried = 0;
        for (NotifyChannelLog entry : failed) {
            NotifyChannel ch = channels.stream()
                    .filter(c -> c.channel().equals(entry.getChannel()))
                    .findFirst().orElse(null);
            if (ch == null) continue;
            entry.setRetryCount(entry.getRetryCount() + 1);
            try {
                Notification n = new Notification();
                n.setId(entry.getNotificationId());
                n.setReceiverId(entry.getReceiverId());
                n.setType("RETRY");
                n.setTitle("通知重发");
                n.setContent("（原通知 #" + entry.getNotificationId() + " 重发）");
                ch.deliver(entry.getReceiverId(), n);
                entry.setStatus("SUCCESS");
                entry.setError(null);
            } catch (Exception e) {
                entry.setError(truncate(e.getMessage()));
            }
            logMapper.updateById(entry);
            retried++;
        }
        return retried;
    }

    private String truncate(String s) {
        return s == null ? null : s.substring(0, Math.min(s.length(), 500));
    }
}
