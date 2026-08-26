package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.student.entity.Course;
import com.example.student.security.DataScopeFilter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    IPage<Course> selectCoursePage(Page<Course> page, @Param("courseName") String courseName,
                                   @Param("teacherId") Long teacherId, @Param("scope") DataScopeFilter scope);

    List<Map<String, Object>> selectCourseExport(@Param("courseName") String courseName,
                                                 @Param("teacherId") Long teacherId,
                                                 @Param("scope") DataScopeFilter scope);

    List<Map<String, Object>> selectIdsByCourseNames(@Param("courseNames") List<String> courseNames,
                                                     @Param("scope") DataScopeFilter scope);

    /** 范围内课程计数（聚合替代全表加载） */
    @Select("""
            <script>
            SELECT COUNT(*) FROM course c WHERE c.deleted = 0
            <if test="scope != null and !scope.unrestricted">
              AND (
                <if test="scope.teacherId != null">c.teacher_id = #{scope.teacherId} OR</if>
                <if test="scope.hasCourseIds and scope.courseIds.size() > 0">c.id IN
                    <foreach collection="scope.courseIds" item="id" open="(" separator="," close=")">#{id}</foreach> OR</if>
                <if test="scope.departmentId != null">EXISTS (SELECT 1 FROM teacher t_scope WHERE t_scope.id = c.teacher_id AND t_scope.deleted = 0 AND t_scope.department_id = #{scope.departmentId}) OR</if>
                1=0
              )
            </if>
            </script>
            """)
    long countScoped(@Param("scope") DataScopeFilter scope);

    /** 范围内全部课程（SQL 级数据范围过滤） */
    @Select("""
            <script>
            SELECT c.* FROM course c WHERE c.deleted = 0
            <if test="scope != null and !scope.unrestricted">
              AND (
                <if test="scope.teacherId != null">c.teacher_id = #{scope.teacherId} OR</if>
                <if test="scope.hasCourseIds and scope.courseIds.size() > 0">c.id IN
                    <foreach collection="scope.courseIds" item="id" open="(" separator="," close=")">#{id}</foreach> OR</if>
                <if test="scope.departmentId != null">EXISTS (SELECT 1 FROM teacher t_scope WHERE t_scope.id = c.teacher_id AND t_scope.deleted = 0 AND t_scope.department_id = #{scope.departmentId}) OR</if>
                1=0
              )
            </if>
            ORDER BY c.id DESC
            </script>
            """)
    List<Course> selectScoped(@Param("scope") DataScopeFilter scope);
}