package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.student.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {

    /** 系部分页（关键字 + schoolId 筛选） */
    IPage<Department> selectDepartmentPage(Page<Department> page,
                                           @Param("keyword") String keyword,
                                           @Param("schoolId") Long schoolId);

    /** 按学校取启用系部（级联下拉） */
    @Select("""
            <script>
            SELECT * FROM department WHERE deleted = 0 AND status = 1
            <if test='schoolId != null'>AND school_id = #{schoolId}</if>
            ORDER BY id
            </script>
            """)
    List<Department> selectEnabled(@Param("schoolId") Long schoolId);

    /** 系部名称映射 */
    @Select("SELECT id, department_name FROM department WHERE deleted = 0")
    List<Department> selectNameMap();
}