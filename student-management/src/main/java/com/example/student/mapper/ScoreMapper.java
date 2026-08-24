package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.student.entity.Score;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface ScoreMapper extends BaseMapper<Score> {

    IPage<Score> selectScorePage(Page<Score> page, @Param("studentId") Long studentId,
                                 @Param("courseId") Long courseId,
                                 @Param("clazzId") Long clazzId);

    /**
     * 成绩统计：平均分、最高分、最低分、及格率(%)。
     * 支持按课程、班级过滤（班级需关联 student 表）。
     */
    Map<String, BigDecimal> selectScoreStats(@Param("courseId") Long courseId,
                                             @Param("clazzId") Long clazzId);

    /** 成绩分段分布：[0,60) / [60,70) / [70,80) / [80,90) / [90,100]，返回 bucket(0-4) 与 cnt */
    List<Map<String, Object>> selectScoreDistribution();

    /** 各班平均分（降序） */
    List<Map<String, Object>> selectAvgByClazz();

    /** 各课程及格率(%)（降序） */
    List<Map<String, Object>> selectPassRateByCourse();

    /** 各课程平均分（降序） */
    List<Map<String, Object>> selectAvgByCourse();

    /** 成绩 TopN 学生（按平均分降序） */
    List<Map<String, Object>> selectTopStudents(@Param("limit") int limit);

    /** 成绩趋势：按月平均分 */
    List<Map<String, Object>> selectScoreTrend();

    /** 及格/不及格人数 */
    Map<String, Object> selectPassFail();

    /** 指定学生的各课程成绩（用于雷达图） */
    List<Map<String, Object>> selectStudentCourseScores(@Param("studentId") Long studentId);
}
