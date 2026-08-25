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
}
