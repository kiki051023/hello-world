package com.example.student.service.channel;

import com.example.student.entity.Notification;
import com.example.student.entity.User;
import com.example.student.mapper.UserMapper;
import com.example.student.service.NotifyChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 邮件渠道（M3-1）：用户 email 非空且 SMTP 已配置才发。
 * spring.mail.host 未配置时 JavaMailSender 为空，supports 直接 false。
 */
@Component
public class EmailNotifyChannel implements NotifyChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailNotifyChannel.class);

    private final UserMapper userMapper;
    private final JavaMailSender mailSender; // null when spring.mail.host unset

    public EmailNotifyChannel(UserMapper userMapper,
                              @Autowired(required = false) JavaMailSender mailSender) {
        this.userMapper = userMapper;
        this.mailSender = mailSender;
    }

    @Override
    public String channel() {
        return "EMAIL";
    }

    @Override
    public boolean supports(Long receiverId) {
        return mailSender != null && emailOf(receiverId) != null;
    }

    @Override
    public void deliver(Long receiverId, Notification n) {
        String email = emailOf(receiverId);
        if (email == null) throw new IllegalStateException("user has no email: " + receiverId);
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("[" + n.getType() + "] " + n.getTitle());
        msg.setText(n.getContent());
        mailSender.send(msg);
        log.info("notify email sent to userId={}", receiverId);
    }

    private String emailOf(Long receiverId) {
        User u = userMapper.selectById(receiverId);
        String email = u == null ? null : u.getEmail();
        return (email == null || email.isBlank()) ? null : email;
    }
}
