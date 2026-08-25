package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.student.entity.Clazz;
import com.example.student.security.DataScopeFilter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ClazzMapper extends BaseMapper<Clazz> {

    IPage<Clazz> selectClazzPage(Page<Clazz> page, @Param("clazzName") String clazzName,
                                 @Param("grade") String grade, @Param("scope") DataScopeFilter scope);

    /** 范围内班级计数（聚合替代全表加载） */
    @Select("""
            <script>
            SELECT COUNT(*) FROM clazz c WHERE c.deleted = 0
            <if test="scope != null and !scope.unrestricted">
              AND ( (scope.departmentId != null AND c.department_id = scope.departmentId)
                 OR (scope.hasClassIds AND c.id IN
                     <foreach collection="scope.classIds" item="id" open="(" separator="," close=")">#{id}</foreach>)
                 OR (scope.teacherId != null AND c.head_teacher_id = scope.teacherId) )
            </if>
            </script>
            """)
    long countScoped(@Param("scope") DataScopeFilter scope);
}
