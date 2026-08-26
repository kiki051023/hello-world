package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.student.entity.Student;
import com.example.student.security.DataScopeFilter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    /** 范围内学生计数（聚合替代全表加载） */
    @org.apache.ibatis.annotations.Select("""
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
}