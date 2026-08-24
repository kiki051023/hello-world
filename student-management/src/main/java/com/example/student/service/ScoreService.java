package com.example.student.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.common.ImportResult;
import com.example.student.entity.Score;
import com.example.student.excel.ScoreExcel;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ScoreService extends IService<Score> {

    IPage<Score> getScorePage(Long current, Long size, Long studentId, Long courseId, Long clazzId);

    void addScore(Score score);

    void updateScore(Score score);

    void deleteScore(Long id);

    Score getScoreById(Long id);

    Map<String, BigDecimal> getScoreStats(Long courseId, Long clazzId);

    List<ScoreExcel> exportScores(Long studentId, Long courseId, Long clazzId);

    ImportResult<ScoreExcel> importScores(MultipartFile file);
}
