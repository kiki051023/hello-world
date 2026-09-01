package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.student.entity.ScheduledTaskLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScheduledTaskLogMapper extends BaseMapper<ScheduledTaskLog> {
}