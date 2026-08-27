package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.student.entity.Student;
import com.example.student.security.DataScopeFilter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {

    IPage<Student> selectStudentPage(Page<Student> page, @Param("name") String name,
                                     @Param("studentNo") String studentNo,
                                     @Param("clazzId") Long clazzId,
                                     @Param("scope") DataScopeFilter scope);

    List<Map<String, Object>> selectStudentExport(@Param("name") String name,
                                                  @Param("studentNo") String studentNo,
                                                  @Param("clazzId") Long clazzId,
                                                  @Param("scope") DataScopeFilter scope);

    List<Map<String, Object>> selectIdsByStudentNos(@Param("studentNos") List<String> studentNos,
                                                    @Param("scope") DataScopeFilter scope);

    /** 范围内学生计数（聚合替代全表加载，统计服务用） */
    @Select("""
            <script>
            SELECT COUNT(*) FROM student s WHERE s.deleted = 0
            <if test="scope != null and !scope.unrestricted">
              AND (
                <if test="scope.selfOnly">s.user_id = #{scope.userId} OR</if>
                <if test="scope.departmentId != null">s.department_id = #{scope.departmentId} OR</if>
                <if test="scope.hasClassIds and scope.classIds.size() > 0">s.clazz_id IN
                    <foreach collection="scope.classIds" item="id" open="(" separator="," close=")">#{id}</foreach> OR</if>
                1=0
              )
            </if>
            </script>
            """)
    long countScoped(@Param("scope") DataScopeFilter scope);

    /** 学生导出计数：scope + 名称/学号/班级筛选（聚合替代全表加载，与 selectStudentExport 同条件） */
    @Select("""
            <script>
            SELECT COUNT(*) FROM student s WHERE s.deleted = 0
            <if test="scope != null and !scope.unrestricted">
              AND (
                <if test="scope.selfOnly">s.user_id = #{scope.userId} OR</if>
                <if test="scope.departmentId != null">s.department_id = #{scope.departmentId} OR</if>
                <if test="scope.hasClassIds and scope.classIds.size() > 0">s.clazz_id IN
                    <foreach collection="scope.classIds" item="id" open="(" separator="," close=")">#{id}</foreach> OR</if>
                <if test="scope.teacherId != null">EXISTS (SELECT 1 FROM score sc_scope JOIN course co_scope ON co_scope.id = sc_scope.course_id WHERE sc_scope.student_id = s.id AND sc_scope.deleted = 0 AND co_scope.teacher_id = #{scope.teacherId}) OR</if>
                1=0
              )
            </if>
            <if test="name != null and name != ''">AND s.name LIKE CONCAT('%', #{name}, '%')</if>
            <if test="studentNo != null and studentNo != ''">AND s.student_no = #{studentNo}</if>
            <if test="clazzId != null">AND s.clazz_id = #{clazzId}</if>
            </script>
            """)
    long countForExport(@Param("name") String name, @Param("studentNo") String studentNo,
                        @Param("clazzId") Long clazzId, @Param("scope") DataScopeFilter scope);
}