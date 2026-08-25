package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.CourseSelection;
import com.example.student.entity.CourseTerm;
import com.example.student.entity.Student;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.CourseSelectionMapper;
import com.example.student.mapper.CourseTermMapper;
import com.example.student.mapper.StudentMapper;
import com.example.student.security.DataScopeFilter;
import com.example.student.security.DataScopeService;
import com.example.student.security.StudentDataScopeService;
import com.example.student.service.CourseSelectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class CourseSelectionServiceImpl extends ServiceImpl<CourseSelectionMapper, CourseSelection>
        implements CourseSelectionService {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private CourseTermMapper courseTermMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private StudentDataScopeService studentDataScopeService;

    @Override
    public Map<String, Object> getPage(long current, long size, Long courseTermId, Long studentId) {
        DataScopeFilter scope = dataScopeService.currentFilter();
        Long scopedStudentId = studentDataScopeService.resolveStudentId(studentId);
        long offset = (current - 1) * size;
        Map<String, Object> result = new HashMap<>();
        result.put("total", baseMapper.countSelectionPage(courseTermId, scopedStudentId, scope));
        result.put("records", baseMapper.selectSelectionPage(offset, size, courseTermId, scopedStudentId, scope));
        return result;
    }

    @Override
    @Transactional
    public void select(CourseSelection selection) {
        // 学生只能选本人课程；管理员/教务可代选
        Long resolvedStudentId = studentDataScopeService.resolveStudentId(selection.getStudentId());
        if (resolvedStudentId == null) throw new BusinessException("学号不能为空");
        if (!resolvedStudentId.equals(selection.getStudentId())
                && !dataScopeService.currentFilter().isUnrestricted()
                && dataScopeService.currentFilter().getTeacherId() == null)
            throw new org.springframework.security.access.AccessDeniedException("无权为他人选课");

        CourseTerm courseTerm = courseTermMapper.selectById(selection.getCourseTermId());
        if (courseTerm == null || courseTerm.getStatus() == null || courseTerm.getStatus() != 1)
            throw new BusinessException("开课不存在或已停开");
        Long selected = baseMapper.selectCount(new LambdaQueryWrapper<CourseSelection>()
                .eq(CourseSelection::getCourseTermId, selection.getCourseTermId())
                .eq(CourseSelection::getStatus, 1));
        if (courseTerm.getCapacity() != null && courseTerm.getCapacity() > 0
                && selected != null && selected >= courseTerm.getCapacity())
            throw new BusinessException("该课程已满员");

        Long exists = baseMapper.selectCount(new LambdaQueryWrapper<CourseSelection>()
                .eq(CourseSelection::getCourseTermId, selection.getCourseTermId())
                .eq(CourseSelection::getStudentId, resolvedStudentId));
        if (exists != null && exists > 0) throw new BusinessException("该生已选择此课程");

        selection.setStudentId(resolvedStudentId);
        if (selection.getSelectionType() == null) selection.setSelectionType("NORMAL");
        selection.setStatus(1);
        baseMapper.insert(selection);
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        CourseSelection existing = baseMapper.selectById(id);
        if (existing == null) throw new BusinessException("选课记录不存在");
        Student student = studentMapper.selectById(existing.getStudentId());
        boolean self = student != null && student.getUserId() != null
                && student.getUserId().equals(dataScopeService.currentFilter().getUserId());
        if (!self && !dataScopeService.canAccessStudent(student))
            throw new org.springframework.security.access.AccessDeniedException("无权取消该选课");
        baseMapper.deleteById(id);
    }
}
