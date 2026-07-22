package com.example.student.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.Teacher;
import com.example.student.mapper.TeacherMapper;
import com.example.student.service.TeacherService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, Teacher> implements TeacherService {

    @Override
    public List<Teacher> getAllTeacher() {
        return baseMapper.selectList(null);
    }

    @Override
    public void addTeacher(Teacher teacher) {
        baseMapper.insert(teacher);
    }

    @Override
    public void updateTeacher(Teacher teacher) {
        baseMapper.updateById(teacher);
    }

    @Override
    public void deleteTeacher(Long id) {
        baseMapper.deleteById(id);
    }
}
