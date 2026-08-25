package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.student.entity.Clazz;
import com.example.student.security.DataScopeFilter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ClazzMapper extends BaseMapper<Clazz> {

    IPage<Clazz> selectClazzPage(Page<Clazz> page, @Param("clazzName") String clazzName,
                                 @Param("grade") String grade, @Param("scope") DataScopeFilter scope);
}
