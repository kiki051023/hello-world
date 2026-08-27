package com.example.student.service;

import com.example.student.entity.Student;
import org.springframework.stereotype.Component;

/**
 * 通知发送辅助组件（P2-D）：
 * 各业务 ServiceImpl 埋点时统一通过学生实体定位接收用户（student.userId = sys_user.id），
 * 学生无关联账号（userId 为 null）时静默跳过。
 */
@Component
public class NotificationSender {

    private final NotificationService notificationService;

    public NotificationSender(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void toStudent(Student student, String type, String title, String content, Long relatedId) {
        if (student == null || student.getUserId() == null) return;
        notificationService.send(student.getUserId(), type, title, content, relatedId);
    }

    public void toUser(Long receiverId, String type, String title, String content, Long relatedId) {
        if (receiverId == null) return;
        notificationService.send(receiverId, type, title, content, relatedId);
    }
}