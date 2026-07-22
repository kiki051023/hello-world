package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.Student;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.StudentMapper;
import com.example.student.service.StudentService;
import org.springframework.stereotype.Service;

@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    @Override
    public IPage<Student> getStudentPage(Long current, Long size, String name, String studentNo, Long clazzId) {
        Page<Student> page = new Page<>(current, size);
        return baseMapper.selectStudentPage(page, name, studentNo, clazzId);
    }

    @Override
    public void addStudent(Student student) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getStudentNo, student.getStudentNo());
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("学号已存在");
        }
        baseMapper.insert(student);
    }

    @Override
    public void updateStudent(Student student) {
        if (student.getId() == null) {
            throw new BusinessException("学生ID不能为空");
        }
        baseMapper.updateById(student);
    }

    @Override
    public void deleteStudent(Long id) {
        baseMapper.deleteById(id);
    }

    @Override
    public Student getStudentById(Long id) {
        return baseMapper.selectById(id);
    }
}
