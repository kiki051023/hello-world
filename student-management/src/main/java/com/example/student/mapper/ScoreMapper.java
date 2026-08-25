package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.student.entity.Score;
import com.example.student.security.DataScopeFilter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface ScoreMapper extends BaseMapper<Score> {

    IPage<Score> selectScorePage(Page<Score> page, @Param("studentId") Long studentId,
                                 @Param("courseId") Long courseId,
                                 @Param("clazzId") Long clazzId, @Param("scope") DataScopeFilter scope);

    default IPage<Score> selectScorePage(Page<Score> page, Long studentId, Long courseId, Long clazzId) {
        return selectScorePage(page, studentId, courseId, clazzId, null);
    }

    /**
     * 成绩统计：平均分、最高分、最低分、及格率(%)。
     * 支持按课程、班级过滤（班级需关联 student 表）。
     */
    Map<String, BigDecimal> selectScoreStats(@Param("studentId") Long studentId,
                                             @Param("courseId") Long courseId,
                                             @Param("clazzId") Long clazzId, @Param("scope") DataScopeFilter scope);

    long selectScopedCount(@Param("scope") DataScopeFilter scope);

    List<Map<String, Object>> selectScoreExport(@Param("studentId") Long studentId,
                                                @Param("courseId") Long courseId,
                                                @Param("clazzId") Long clazzId, @Param("scope") DataScopeFilter scope);

    default List<Map<String, Object>> selectScoreExport(Long studentId, Long courseId, Long clazzId) {
        return selectScoreExport(studentId, courseId, clazzId, null);
    }

    List<Map<String, Object>> selectExistingStudentCoursePairs(@Param("studentIds") List<Long> studentIds,
                                                                @Param("courseIds") List<Long> courseIds);

    /** 成绩分段分布：[0,60) / [60,70) / [70,80) / [80,90) / [90,100]，返回 bucket(0-4) 与 cnt */
    List<Map<String, Object>> selectScoreDistribution(@Param("scope") DataScopeFilter scope);

    /** 各班平均分（降序） */
    List<Map<String, Object>> selectAvgByClazz(@Param("scope") DataScopeFilter scope);

    /** 各课程及格率(%)（降序） */
    List<Map<String, Object>> selectPassRateByCourse(@Param("scope") DataScopeFilter scope);

    /** 各课程平均分（降序） */
    List<Map<String, Object>> selectAvgByCourse(@Param("scope") DataScopeFilter scope);

    /** 成绩 TopN 学生（按平均分降序） */
    List<Map<String, Object>> selectTopStudents(@Param("limit") int limit, @Param("scope") DataScopeFilter scope);

    /** 成绩趋势：按月平均分 */
    List<Map<String, Object>> selectScoreTrend(@Param("scope") DataScopeFilter scope);

    /** 及格/不及格人数 */
    Map<String, Object> selectPassFail(@Param("scope") DataScopeFilter scope);

    /** 指定学生的各课程成绩（用于雷达图） */
    List<Map<String, Object>> selectStudentCourseScores(@Param("studentId") Long studentId);

    /** 学生学分聚合：仅统计有效成绩（active=1 且 APPROVED），返回 earnedCredits/totalCredits/passCount/failCount */
    Map<String, Object> selectStudentCreditSummary(@Param("studentId") Long studentId,
                                                   @Param("termId") Long termId);

    /** 按学生列表批量计算学分（用于班级/课程维度汇总） */
    List<Map<String, Object>> selectCreditSummaryByClazz(@Param("clazzId") Long clazzId,
                                                         @Param("termId") Long termId);

    /** 每生有效不及格课程数（预警用） */
    List<Map<String, Object>> countFailCoursesPerStudent();

    /** 导出前计数（同导出过滤条件） */
    long countForExport(@Param("studentId") Long studentId,
                        @Param("courseId") Long courseId,
                        @Param("clazzId") Long clazzId,
                        @Param("scope") DataScopeFilter scope);

    default long countForExport(Long studentId, Long courseId, Long clazzId) {
        return countForExport(studentId, courseId, clazzId, null);
    }
}
