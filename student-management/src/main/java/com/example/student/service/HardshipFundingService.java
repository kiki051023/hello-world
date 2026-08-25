package com.example.student.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.HardshipFunding;

import java.util.Map;

public interface HardshipFundingService extends IService<HardshipFunding> {

    /** 列表：敏感字段按角色脱敏后返回 */
    Map<String, Object> getPage(long current, long size, Long studentId, String status);

    void addApplication(HardshipFunding funding);

    void review(Long id, boolean pass, String comment);
}
