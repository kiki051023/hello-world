package com.example.student.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.LeaveRequest;

import java.util.Map;

public interface LeaveRequestService extends IService<LeaveRequest> {

    Map<String, Object> getPage(long current, long size, Long studentId, String status);

    /** 学生提交申请（学生仅能替本人提交） */
    void submit(LeaveRequest request);

    /** 辅导员/教务审批（仅负责班级或全校权限） */
    void approve(Long id, boolean pass, String comment);

    /** 销假；超时未销假由定时扫描标记 OVERDUE */
    void returnBack(Long id);
}
