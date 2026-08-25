package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.Course;
import com.example.student.entity.CourseTerm;
import com.example.student.mapper.AcademicTermMapper;
import com.example.student.mapper.CourseMapper;
import com.example.student.mapper.CourseTermMapper;
import com.example.student.security.DataScopeFilter;
import com.example.student.security.DataScopeService;
import com.example.student.service.CourseTermService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class CourseTermServiceImpl extends ServiceImpl<CourseTermMapper, CourseTerm> implements CourseTermService {

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private AcademicTermMapper academicTermMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Override
    public Map<String, Object> getPage(long current, long size, Long courseId, Long termId) {
        DataScopeFilter scope = dataScopeService.currentFilter();
        long offset = (current - 1) * size;
        Map<String, Object> result = new HashMap<>();
        result.put("total", baseMapper.countCourseTermPage(courseId, termId, scope));
        result.put("records", baseMapper.selectCourseTermPage(offset, size, courseId, termId, scope));
        return result;
    }

    @Override
    public CourseTerm getByIdChecked(Long id) {
        CourseTerm courseTerm = baseMapper.selectById(id);
        if (courseTerm == null) return null;
        Course course = courseMapper.selectById(courseTerm.getCourseId());
        if (!dataScopeService.canAccessCourse(course))
            throw new org.springframework.security.access.AccessDeniedException("无权查看该课程开设");
        return courseTerm;
    }

    @Override
    @Transactional
    public void addCourseTerm(CourseTerm courseTerm) {
        if (academicTermMapper.selectById(courseTerm.getTermId()) == null)
            throw new com.example.student.exception.BusinessException("学期不存在");
        Course course = courseMapper.selectById(courseTerm.getCourseId());
        if (course == null) throw new com.example.student.exception.BusinessException("课程不存在");
        if (!dataScopeService.canAccessCourse(course))
            throw new org.springframework.security.access.AccessDeniedException("无权为该课程建立开课");
        Long exists = baseMapper.selectCount(new LambdaQueryWrapper<CourseTerm>()
                .eq(CourseTerm::getTermId, courseTerm.getTermId())
                .eq(CourseTerm::getCourseId, courseTerm.getCourseId()));
        if (exists != null && exists > 0)
            throw new com.example.student.exception.BusinessException("该学期已存在此课程开设");
        baseMapper.insert(courseTerm);
    }

    @Override
    @Transactional
    public void updateCourseTerm(CourseTerm courseTerm) {
        CourseTerm existing = getByIdChecked(courseTerm.getId());
        if (existing == null) throw new com.example.student.exception.BusinessException("开课记录不存在");
        existing.setCapacity(courseTerm.getCapacity() != null ? courseTerm.getCapacity() : existing.getCapacity());
        existing.setStatus(courseTerm.getStatus() != null ? courseTerm.getStatus() : existing.getStatus());
        baseMapper.updateById(existing);
    }

    @Override
    @Transactional
    public void deleteCourseTerm(Long id) {
        getByIdChecked(id);
        baseMapper.deleteById(id);
    }
}
