package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.student.entity.AcademicTerm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AcademicTermMapper extends BaseMapper<AcademicTerm> {

    /** 将所有学期置为非当前（切换当前学期时调用） */
    @Update("UPDATE academic_term SET status = 0 WHERE status = 1")
    int resetCurrentFlag();
}
