package com.example.student.service;

import java.util.List;
import java.util.Map;

/**
 * 统计分析与数据大屏聚合服务。
 */
public interface StatisticsService {

    /** 概览：学生/班级/课程/教师/成绩总数 + 总平均分/及格率/最高最低分 */
    Map<String, Object> getOverview();

    /** 成绩深度分析：分段分布、班级平均分、课程及格率、课程平均分、Top学生、趋势、及格/不及格 */
    Map<String, Object> getScoreAnalysis();

    /** 单个学生的各课程成绩（雷达图） */
    Map<String, Object> getStudentRadar(Long studentId);
}
