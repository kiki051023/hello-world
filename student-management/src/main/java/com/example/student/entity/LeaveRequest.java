package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("leave_request")
public class LeaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    /** PERSONAL/SICK/OTHER */
    private String leaveType;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String reason;

    private String destination;

    /** PENDING/APPROVED/REJECTED/CANCELLED/RETURNED/OVERDUE */
    private String status;

    private Long approverId;

    private String approverName;

    private String approveComment;

    private LocalDateTime approvedAt;

    private LocalDateTime returnedAt;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String studentNo;

    @TableField(exist = false)
    private String studentName;

    @TableField(exist = false)
    private String clazzName;
}
