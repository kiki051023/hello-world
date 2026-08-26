package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.student.entity.Clazz;
import com.example.student.security.DataScopeFilter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ClazzMapper extends BaseMapper<Clazz> {

    IPage<Clazz> selectClazzPage(Page<Clazz> page, @Param("clazzName") String clazzName,
                                 @Param("grade") String grade, @Param("scope") DataScopeFilter scope);

    /** 范围内班级计数（聚合替代全表加载） */
    @Select("""
            <script>
            SELECT COUNT(*) FROM clazz c WHERE c.deleted = 0
            <if test="scope != null and !scope.unrestricted">
              AND (
                <if test="scope.departmentId != null">c.department_id = #{scope.departmentId} OR</if>
                <if test="scope.hasClassIds and scope.classIds.size() > 0">c.id IN
                    <foreach collection="scope.classIds" item="id" open="(" separator="," close=")">#{id}</foreach> OR</if>
                <if test="scope.teacherId != null">c.head_teacher_id = #{scope.teacherId} OR</if>
                1=0
              )
            </if>
            </script>
            """)
    long countScoped(@Param("scope") DataScopeFilter scope);

    /** 范围内全部班级（SQL 级数据范围过滤，替代 selectList(null)+内存过滤） */
    @Select("""
            <script>
            SELECT c.* FROM clazz c WHERE c.deleted = 0
            <if test="scope != null and !scope.unrestricted">
              AND (
                <if test="scope.departmentId != null">c.department_id = #{scope.departmentId} OR</if>
                <if test="scope.hasClassIds and scope.classIds.size() > 0">c.id IN
                    <foreach collection="scope.classIds" item="id" open="(" separator="," close=")">#{id}</foreach> OR</if>
                <if test="scope.teacherId != null">c.head_teacher_id = #{scope.teacherId} OR</if>
                1=0
              )
            </if>
            ORDER BY c.id DESC
            </script>
            """)
    List<Clazz> selectScoped(@Param("scope") DataScopeFilter scope);

    /** 范围内班级 id+name 投影（学生导入名称映射用） */
    @Select("""
            <script>
            SELECT c.id, c.clazz_name AS clazzName FROM clazz c WHERE c.deleted = 0
            <if test="scope != null and !scope.unrestricted">
              AND (
                <if test="scope.departmentId != null">c.department_id = #{scope.departmentId} OR</if>
                <if test="scope.hasClassIds and scope.classIds.size() > 0">c.id IN
                    <foreach collection="scope.classIds" item="id" open="(" separator="," close=")">#{id}</foreach> OR</if>
                <if test="scope.teacherId != null">c.head_teacher_id = #{scope.teacherId} OR</if>
                1=0
              )
            </if>
            </script>
            """)
    List<Map<String, Object>> selectScopedNameMap(@Param("scope") DataScopeFilter scope);

    /** 指定 ID 集合的教师姓名投影（单条 SQL，替代教师全表加载） */
    @Select("""
            <script>
            SELECT id, name FROM teacher WHERE deleted = 0
            <if test="ids != null and ids.size() > 0">
              AND id IN <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </if>
            </script>
            """)
    List<Map<String, Object>> selectNamesByIds(@Param("ids") List<Long> ids);
}