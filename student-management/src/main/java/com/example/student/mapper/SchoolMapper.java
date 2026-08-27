package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.student.entity.School;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SchoolMapper extends BaseMapper<School> {

    /** 学校分页（名称/编码筛选） */
    IPage<School> selectSchoolPage(Page<School> page,
                                   @Param("keyword") String keyword);

    /** 启用状态的学校（下拉） */
    @Select("SELECT * FROM school WHERE deleted = 0 AND status = 1 ORDER BY id")
    List<School> selectEnabled();

    /** 学校名称映射（列表冗余展示） */
    @Select("SELECT id, school_name FROM school WHERE deleted = 0")
    List<School> selectNameMap();
}