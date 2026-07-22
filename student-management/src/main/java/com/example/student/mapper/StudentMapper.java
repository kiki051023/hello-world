package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.student.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {

    IPage<Student> selectStudentPage(Page<Student> page, @Param("name") String name,
                                     @Param("studentNo") String studentNo,
                                     @Param("clazzId") Long clazzId);
}
