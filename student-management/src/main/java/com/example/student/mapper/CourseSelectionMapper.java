package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.student.entity.CourseSelection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CourseSelectionMapper extends BaseMapper<CourseSelection> {

    List<Map<String, Object>> selectSelectionPage(@Param("offset") long offset, @Param("size") long size,
                                                  @Param("courseTermId") Long courseTermId,
                                                  @Param("studentId") Long studentId,
                                                  @Param("scope") com.example.student.security.DataScopeFilter scope);

    long countSelectionPage(@Param("courseTermId") Long courseTermId, @Param("studentId") Long studentId,
                            @Param("scope") com.example.student.security.DataScopeFilter scope);

    /**
     * M4-2 时间冲突检测：某学生已选课程中，与目标 courseTerm 同 (weekDay, period) 重叠的数量。
     * 节次用重叠区间判断（"1-2" 与 "2-3" 冲突），任一课程未设时间槽则跳过（NULL 不参与）。
     */
    @org.apache.ibatis.annotations.Select("""
            <script>
            SELECT COUNT(*) FROM course_selection cs
            JOIN course_term ct ON ct.id = cs.course_term_id AND ct.deleted = 0
            JOIN teaching_task tt ON tt.course_term_id = ct.id AND tt.deleted = 0
            JOIN teaching_task target ON target.course_term_id = #{courseTermId} AND target.deleted = 0
            WHERE cs.student_id = #{studentId} AND cs.status = 1 AND cs.deleted = 0
              AND tt.week_day IS NOT NULL AND target.week_day IS NOT NULL
              AND tt.week_day = target.week_day
              AND tt.period IS NOT NULL AND target.period IS NOT NULL
              AND CAST(SUBSTRING_INDEX(tt.period, '-', 1) AS UNSIGNED) &lt;= CAST(SUBSTRING_INDEX(target.period, '-', -1) AS UNSIGNED)
              AND CAST(SUBSTRING_INDEX(target.period, '-', 1) AS UNSIGNED) &lt;= CAST(SUBSTRING_INDEX(tt.period, '-', -1) AS UNSIGNED)
            </script>
            """)
    long countTimeConflict(@Param("studentId") Long studentId, @Param("courseTermId") Long courseTermId);

    /** M4-2 学生周课表：已选课程的时间槽（join teaching_task） */
    @org.apache.ibatis.annotations.Select("""
            SELECT c.course_name, t.name AS teacher_name, ct.id AS course_term_id,
                   tt.week_day, tt.period, tt.classroom
            FROM course_selection cs
            JOIN course_term ct ON ct.id = cs.course_term_id AND ct.deleted = 0
            JOIN course c ON c.id = ct.course_id
            LEFT JOIN teaching_task tt ON tt.course_term_id = ct.id AND tt.deleted = 0 AND tt.week_day IS NOT NULL
            LEFT JOIN teacher t ON t.id = tt.teacher_id
            WHERE cs.student_id = #{studentId} AND cs.status = 1 AND cs.deleted = 0
            ORDER BY tt.week_day, tt.period
            """)
    List<Map<String, Object>> selectStudentTimetable(@Param("studentId") Long studentId);
}
