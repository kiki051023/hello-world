package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.student.entity.TeachingTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface TeachingTaskMapper extends BaseMapper<TeachingTask> {

    List<Map<String, Object>> selectTeachingTaskPage(@Param("offset") long offset, @Param("size") long size,
                                                     @Param("teacherId") Long teacherId, @Param("termId") Long termId,
                                                     @Param("clazzId") Long clazzId,
                                                     @Param("scope") com.example.student.security.DataScopeFilter scope);

    long countTeachingTaskPage(@Param("teacherId") Long teacherId, @Param("termId") Long termId,
                               @Param("clazzId") Long clazzId,
                               @Param("scope") com.example.student.security.DataScopeFilter scope);
}
