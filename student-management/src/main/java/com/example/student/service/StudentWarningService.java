package com.example.student.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.StudentWarning;

import java.util.Map;

public interface StudentWarningService extends IService<StudentWarning> {

    Map<String, Object> getPage(long current, long size, Long studentId, String status);

    /** 按可配置规则扫描生成预警（成绩挂科数、请假超时等），幂等：已有同键 ACTIVE 预警不重复生成 */
    int generateByRule(String warningType, String ruleKey, int threshold, Map<Long, Integer> studentValueMap);

    void handle(Long id, boolean dismiss, String note);
}
