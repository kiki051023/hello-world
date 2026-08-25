package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.student.entity.LeaveRequest;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LeaveRequestMapper extends BaseMapper<LeaveRequest> {
}
