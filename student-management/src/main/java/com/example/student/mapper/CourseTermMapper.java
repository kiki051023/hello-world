package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.student.entity.CourseTerm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CourseTermMapper extends BaseMapper<CourseTerm> {

    List<Map<String, Object>> selectCourseTermPage(@Param("offset") long offset, @Param("size") long size,
                                                   @Param("courseId") Long courseId, @Param("termId") Long termId,
                                                   @Param("scope") com.example.student.security.DataScopeFilter scope);

    long countCourseTermPage(@Param("courseId") Long courseId, @Param("termId") Long termId,
                             @Param("scope") com.example.student.security.DataScopeFilter scope);
}
