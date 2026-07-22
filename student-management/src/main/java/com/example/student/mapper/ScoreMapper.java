package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.student.entity.Score;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScoreMapper extends BaseMapper<Score> {

    IPage<Score> selectScorePage(Page<Score> page, @Param("studentId") Long studentId,
                                 @Param("courseId") Long courseId,
                                 @Param("clazzId") Long clazzId);
}
