package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.Course;
import com.example.student.mapper.CourseMapper;
import com.example.student.service.CourseService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    @Override
    public IPage<Course> getCoursePage(Long current, Long size, String courseName, Long teacherId) {
        Page<Course> page = new Page<>(current, size);
        return baseMapper.selectCoursePage(page, courseName, teacherId);
    }

    @Override
    public void addCourse(Course course) {
        baseMapper.insert(course);
    }

    @Override
    public void updateCourse(Course course) {
        baseMapper.updateById(course);
    }

    @Override
    public void deleteCourse(Long id) {
        baseMapper.deleteById(id);
    }

    @Override
    public List<Course> getAllCourse() {
        return baseMapper.selectList(null);
    }
}
