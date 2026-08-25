package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.student.entity.Teacher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TeacherMapper extends BaseMapper<Teacher> {

    @Select("""
            <script>
            SELECT COUNT(*) FROM teacher t WHERE t.deleted = 0
            <if test="scope != null and !scope.unrestricted">
              AND ( scope.teacherId != null AND t.id = scope.teacherId
                 OR (scope.departmentId != null AND t.department_id = scope.departmentId) )
            </if>
            </script>
            """)
    long countScoped(@Param("scope") com.example.student.security.DataScopeFilter scope);
}
