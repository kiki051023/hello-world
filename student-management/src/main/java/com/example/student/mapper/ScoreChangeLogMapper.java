package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.student.entity.ScoreChangeLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ScoreChangeLogMapper extends BaseMapper<ScoreChangeLog> {

    List<ScoreChangeLog> selectByScoreId(@Param("scoreId") Long scoreId);
}
