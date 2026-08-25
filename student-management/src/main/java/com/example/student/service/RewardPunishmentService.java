package com.example.student.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.RewardPunishment;

import java.util.Map;

public interface RewardPunishmentService extends IService<RewardPunishment> {

    Map<String, Object> getPage(long current, long size, Long studentId, String rpType);

    void addRecord(RewardPunishment record);

    /** 解除处分（记录解除日期，不删除原记录） */
    void revoke(Long id, String note);
}
