package com.example.student.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.common.ImportResult;
import com.example.student.entity.Course;
import com.example.student.entity.Teacher;
import com.example.student.exception.BusinessException;
import com.example.student.excel.CourseExcel;
import com.example.student.mapper.CourseMapper;
import com.example.student.mapper.TeacherMapper;
import com.example.student.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    @Autowired
    private TeacherMapper teacherMapper;

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

    @Override
    public List<CourseExcel> exportCourses(String courseName, Long teacherId) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        if (courseName != null && !courseName.isEmpty()) wrapper.like(Course::getCourseName, courseName);
        if (teacherId != null) wrapper.eq(Course::getTeacherId, teacherId);
        List<Course> list = baseMapper.selectList(wrapper);
        Map<Long, String> teacherMap = teacherMapper.selectList(null).stream()
                .collect(Collectors.toMap(Teacher::getId, Teacher::getName, (a, b) -> a));
        List<CourseExcel> result = new ArrayList<>();
        for (Course c : list) {
            CourseExcel e = new CourseExcel();
            e.setCourseNo(c.getCourseNo());
            e.setCourseName(c.getCourseName());
            e.setCredit(c.getCredit());
            e.setTeacherName(c.getTeacherId() != null ? teacherMap.get(c.getTeacherId()) : null);
            result.add(e);
        }
        return result;
    }

    @Override
    public ImportResult<CourseExcel> importCourses(MultipartFile file) {
        ImportResult<CourseExcel> result = new ImportResult<>();
        try {
            List<?> rows = EasyExcel.read(file.getInputStream()).head(CourseExcel.class).sheet().doReadSync();
            List<CourseExcel> data = rows.stream().map(o -> (CourseExcel) o).collect(Collectors.toList());
            result.setTotal(data.size());
            Set<String> existing = baseMapper.selectList(null).stream()
                    .map(Course::getCourseName).collect(Collectors.toSet());
            Map<String, Long> teacherNameMap = teacherMapper.selectList(null).stream()
                    .collect(Collectors.toMap(Teacher::getName, Teacher::getId, (a, b) -> a));
            List<Course> entities = new ArrayList<>();
            for (CourseExcel e : data) {
                if (e.getCourseName() == null || e.getCourseName().isEmpty()) {
                    e.setErrorMessage("课程名称不能为空");
                    result.getFailRows().add(e);
                    continue;
                }
                if (existing.contains(e.getCourseName())) {
                    e.setErrorMessage("课程已存在(" + e.getCourseName() + ")");
                    result.getFailRows().add(e);
                    continue;
                }
                Course c = new Course();
                c.setCourseNo(e.getCourseNo());
                c.setCourseName(e.getCourseName());
                c.setCredit(e.getCredit());
                if (e.getTeacherName() != null && !e.getTeacherName().isEmpty()) {
                    Long tid = teacherNameMap.get(e.getTeacherName());
                    if (tid == null) {
                        e.setErrorMessage("任课教师不存在(" + e.getTeacherName() + ")");
                        result.getFailRows().add(e);
                        continue;
                    }
                    c.setTeacherId(tid);
                }
                entities.add(c);
            }
            if (!entities.isEmpty()) {
                saveBatch(entities);
                result.setSuccessCount(entities.size());
            }
        } catch (IOException ex) {
            throw new BusinessException("Excel 解析失败: " + ex.getMessage());
        }
        return result;
    }
}
