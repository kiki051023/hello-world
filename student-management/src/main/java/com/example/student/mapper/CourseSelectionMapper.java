package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.student.entity.CourseSelection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CourseSelectionMapper extends BaseMapper<CourseSelection> {

    List<Map<String, Object>> selectSelectionPage(@Param("offset") long offset, @Param("size") long size,
                                                  @Param("courseTermId") Long courseTermId,
                                                  @Param("studentId") Long studentId,
                                                  @Param("scope") com.example.student.security.DataScopeFilter scope);

    long countSelectionPage(@Param("courseTermId") Long courseTermId, @Param("studentId") Long studentId,
                            @Param("scope") com.example.student.security.DataScopeFilter scope);
}
