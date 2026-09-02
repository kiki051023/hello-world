package com.example.student.service.channel;

import com.example.student.entity.Notification;
import com.example.student.service.NotifyChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 企业微信群机器人 webhook（M3-1）：群发渠道，不区分接收人，
 * webhook URL 未配置时 supports=false。成本为零（企微自带）。
 */
@Component
public class WeComNotifyChannel implements NotifyChannel {

    private static final Logger log = LoggerFactory.getLogger(WeComNotifyChannel.class);

    private final RestTemplate rest = new RestTemplate();

    /** notify.wecom-webhook: 企业微信群机器人地址，空则禁用 */
    @Value("${notify.wecom-webhook:}")
    private String webhook;

    @Override
    public String channel() {
        return "WECOM";
    }

    @Override
    public boolean supports(Long receiverId) {
        return webhook != null && !webhook.isBlank();
    }

    @Override
    public void deliver(Long receiverId, Notification n) {
        Map<String, Object> body = Map.of(
                "msgtype", "text",
                "text", Map.of("content", "[" + n.getType() + "] " + n.getTitle() + "\n" + n.getContent()));
        rest.postForObject(webhook, body, String.class);
        log.info("notify wecom sent for userId={}", receiverId);
    }
}
