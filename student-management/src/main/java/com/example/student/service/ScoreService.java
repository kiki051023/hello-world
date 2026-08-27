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

    IPage<Score> getScorePage(Long current, Long size, Long studentId, Long courseId, Long clazzId, Long termId, String studentName, String scoreStatus);

    void addScore(Score score);

    void updateScore(Score score);

    void deleteScore(Long id);

    Score getScoreById(Long id);

    Map<String, BigDecimal> getScoreStats(Long courseId, Long clazzId, Long termId);

    List<ScoreExcel> exportScores(Long studentId, Long courseId, Long clazzId, Long termId);

    ImportResult<ScoreExcel> importScores(MultipartFile file);

    /** 导出前计数（判断是否超异步阈值） */
    long countForExport(Long studentId, Long courseId, Long clazzId, Long termId);

    /** 每生有效不及格课程数（active=1 且 APPROVED，每课取最高分），供预警规则扫描 */
    Map<Long, Integer> countFailCoursesPerStudent();
}
