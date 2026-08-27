package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.student.entity.Major;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MajorMapper extends BaseMapper<Major> {

    /** 专业分页（关键字 + departmentId 筛选） */
    IPage<Major> selectMajorPage(Page<Major> page,
                                 @Param("keyword") String keyword,
                                 @Param("departmentId") Long departmentId);

    /** 按系部取启用专业（级联下拉） */
    @Select("""
            <script>
            SELECT * FROM major WHERE deleted = 0 AND status = 1
            <if test='departmentId != null'>AND department_id = #{departmentId}</if>
            ORDER BY id
            </script>
            """)
    List<Major> selectEnabled(@Param("departmentId") Long departmentId);

    /** 专业名称映射 */
    @Select("SELECT id, major_name FROM major WHERE deleted = 0")
    List<Major> selectNameMap();
}