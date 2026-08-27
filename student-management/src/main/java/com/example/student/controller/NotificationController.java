package com.example.student.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.student.common.PageQuery;
import com.example.student.common.PageResult;
import com.example.student.common.Result;
import com.example.student.entity.Notification;
import com.example.student.security.AuthorizationService;
import com.example.student.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 通知中心（P2-D）：任何登录用户的自有通知。
 * 端点：分页 / 未读数 / 单条已读 / 全部已读。
 */
@Tag(name = "通知中心")
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuthorizationService authorizationService;

    @Operation(summary = "我的通知分页（时间倒序，可按已读筛选）")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/page")
    public Result<PageResult<Notification>> page(@Valid @ModelAttribute PageQuery pageQuery,
                                                 @RequestParam(required = false) Integer isRead) {
        Long receiverId = authorizationService.currentUser().getId();
        IPage<Notification> page = notificationService.getNotificationPage(
                pageQuery.getCurrent(), pageQuery.getSize(), receiverId, isRead);
        return Result.success(PageResult.of(page.getTotal(), page.getRecords(),
                page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "未读通知数（红点）")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/unread-count")
    public Result<Map<String, Object>> unreadCount() {
        Long receiverId = authorizationService.currentUser().getId();
        Map<String, Object> data = new HashMap<>();
        data.put("count", notificationService.countUnread(receiverId));
        return Result.success(data);
    }

    @Operation(summary = "标记单条已读")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id, authorizationService.currentUser().getId());
        return Result.success();
    }

    @Operation(summary = "全部标记已读")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/read-all")
    public Result<Map<String, Object>> markAllRead() {
        Long receiverId = authorizationService.currentUser().getId();
        int updated = notificationService.markAllRead(receiverId);
        Map<String, Object> data = new HashMap<>();
        data.put("updated", updated);
        return Result.success(data);
    }
}