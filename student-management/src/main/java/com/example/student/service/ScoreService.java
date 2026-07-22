package com.example.student.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.Score;

import java.math.BigDecimal;
import java.util.Map;

public interface ScoreService extends IService<Score> {

    IPage<Score> getScorePage(Long current, Long size, Long studentId, Long courseId, Long clazzId);

    void addScore(Score score);

    void updateScore(Score score);

    void deleteScore(Long id);

    Map<String, BigDecimal> getScoreStats(Long courseId, Long clazzId);
}
