package com.example.student.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.PracticeLog;

import java.util.Map;

public interface PracticeLogService extends IService<PracticeLog> {

    Map<String, Object> getPage(long current, long size, Long assignmentId, String status);

    /** 学生提交周记（仅本人实习） */
    void submit(PracticeLog log);

    /** 指导教师点评（仅负责的实习） */
    void comment(Long id, String comment);
}
