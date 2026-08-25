package com.example.student.service;

import com.example.student.entity.StudentStatusHistory;

import java.util.List;
import java.util.Map;

/** 学籍异动：状态/转专业/转班/休学/复学/毕业，全部写入历史表。 */
public interface StudentStatusService {

    /** 通用异动入口：校验数据范围 → 更新 student 字段 → 追加历史 */
    void applyChange(Long studentId, String changeType, String oldValue, String newValue, String reason);

    List<StudentStatusHistory> history(Long studentId);

    Map<String, Object> historyPage(long current, long size, Long studentId);
}
