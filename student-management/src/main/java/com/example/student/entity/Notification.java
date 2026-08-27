package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户通知（P2-D）：待办红点 + 通知面板。
 * 触发点：请假审批 / 成绩审核 / 资助审批 / 学业预警 / 实习分配。
 */
@Data
@TableName("sys_notification")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收用户 ID */
    private Long receiverId;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 类型：LEAVE/SCORE/FUND/WARNING/PRACTICE/SYSTEM */
    private String type;

    /** 关联业务 ID（跳转用） */
    private Long relatedId;

    /** 是否已读：0-未读 1-已读 */
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}