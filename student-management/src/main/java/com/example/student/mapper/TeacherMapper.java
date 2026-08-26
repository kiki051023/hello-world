package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.student.entity.Teacher;
import com.example.student.security.DataScopeFilter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface TeacherMapper extends BaseMapper<Teacher> {

    /** 范围内教师计数（聚合替代全表加载） */
    @Select("""
            <script>
            SELECT COUNT(*) FROM teacher t WHERE t.deleted = 0
            <if test="scope != null and !scope.unrestricted">
              AND (
                <if test="scope.teacherId != null">t.id = #{scope.teacherId} OR</if>
                <if test="scope.departmentId != null">t.department_id = #{scope.departmentId} OR</if>
                1=0
              )
            </if>
            </script>
            """)
    long countScoped(@Param("scope") DataScopeFilter scope);

    /** 范围内全部教师（SQL 级数据范围过滤，替代 selectList(null)+内存过滤） */
    @Select("""
            <script>
            SELECT t.* FROM teacher t WHERE t.deleted = 0
            <if test="scope != null and !scope.unrestricted">
              AND (
                <if test="scope.teacherId != null">t.id = #{scope.teacherId} OR</if>
                <if test="scope.departmentId != null">t.department_id = #{scope.departmentId} OR</if>
                1=0
              )
            </if>
            ORDER BY t.id DESC
            </script>
            """)
    List<Teacher> selectScoped(@Param("scope") DataScopeFilter scope);

    /** 全体在职教师的 id+name 轻量投影（导入名称映射用；仅取两列，非全表实体加载） */
    @Select("SELECT id, name FROM teacher WHERE deleted = 0")
    List<Map<String, Object>> selectAllNames();

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
