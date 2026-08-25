package com.example.student.service;

import com.example.student.entity.Score;
import com.example.student.entity.ScoreChangeLog;

import java.util.List;
import java.util.Map;

/** 成绩生命周期：草稿→提交→审核/退回；补考/重修独立建记录；学分聚合查询。 */
public interface ScoreLifecycleService {

    Score createDraft(Score score, String reason);

    Score submit(Long scoreId);

    Score approve(Long scoreId);

    Score reject(Long scoreId, String reason);

    Score voidScore(Long scoreId, String reason);

    /** 教师修改已提交/已退回成绩的分数（产生变更历史，不覆盖原始值） */
    Score changeScoreValue(Long scoreId, java.math.BigDecimal newScore, String reason);

    Score createMakeup(Long originalScoreId, java.math.BigDecimal newScore, Long courseTermId);

    Score createRetake(Long originalScoreId, java.math.BigDecimal newScore, Long courseTermId, Long newCourseTermId);

    List<ScoreChangeLog> changeHistory(Long scoreId);

    Map<String, Object> creditSummary(Long studentId, Long termId);

    List<Map<String, Object>> clazzCreditSummary(Long clazzId, Long termId);
}
