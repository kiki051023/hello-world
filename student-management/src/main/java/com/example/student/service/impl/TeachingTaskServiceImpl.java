package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.TeachingTask;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.CourseTermMapper;
import com.example.student.mapper.TeacherMapper;
import com.example.student.mapper.TeachingTaskMapper;
import com.example.student.security.DataScopeFilter;
import com.example.student.security.DataScopeService;
import com.example.student.service.TeachingTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class TeachingTaskServiceImpl extends ServiceImpl<TeachingTaskMapper, TeachingTask>
        implements TeachingTaskService {

    @Autowired
    private CourseTermMapper courseTermMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Override
    public Map<String, Object> getPage(long current, long size, Long teacherId, Long termId, Long clazzId) {
        DataScopeFilter scope = dataScopeService.currentFilter();
        long offset = (current - 1) * size;
        Map<String, Object> result = new HashMap<>();
        result.put("total", baseMapper.countTeachingTaskPage(teacherId, termId, clazzId, scope));
        result.put("records", baseMapper.selectTeachingTaskPage(offset, size, teacherId, termId, clazzId, scope));
        return result;
    }

    @Override
    public TeachingTask getByIdChecked(Long id) {
        TeachingTask task = baseMapper.selectById(id);
        if (task == null) return null;
        if (!dataScopeService.canAccessTeacher(teacherMapper.selectById(task.getTeacherId())))
            throw new org.springframework.security.access.AccessDeniedException("无权查看该教学任务");
        return task;
    }

    @Override
    @Transactional
    public void addTeachingTask(TeachingTask task) {
        validate(task);
        if (!dataScopeService.canAssignTeacher(task.getTeacherId()))
            throw new org.springframework.security.access.AccessDeniedException("无权为该教师安排任务");
        Long exists = baseMapper.selectCount(new LambdaQueryWrapper<TeachingTask>()
                .eq(TeachingTask::getCourseTermId, task.getCourseTermId())
                .eq(TeachingTask::getTeacherId, task.getTeacherId())
                .eq(task.getClazzId() != null, TeachingTask::getClazzId, task.getClazzId())
                .isNull(task.getClazzId() == null, TeachingTask::getClazzId));
        if (exists != null && exists > 0)
            throw new BusinessException("该教师在同一开课下已存在相同班级的任务");
        baseMapper.insert(task);
    }

    @Override
    @Transactional
    public void updateTeachingTask(TeachingTask task) {
        TeachingTask existing = getByIdChecked(task.getId());
        if (existing == null) throw new BusinessException("教学任务不存在");
        existing.setTaskRole(task.getTaskRole() != null ? task.getTaskRole() : existing.getTaskRole());
        existing.setWeeklyHours(task.getWeeklyHours() != null ? task.getWeeklyHours() : existing.getWeeklyHours());
        existing.setStatus(task.getStatus() != null ? task.getStatus() : existing.getStatus());
        baseMapper.updateById(existing);
    }

    @Override
    @Transactional
    public void deleteTeachingTask(Long id) {
        TeachingTask existing = getByIdChecked(id);
        if (existing == null) throw new BusinessException("教学任务不存在");
        if (!dataScopeService.canAssignTeacher(existing.getTeacherId()))
            throw new org.springframework.security.access.AccessDeniedException("无权删除该教学任务");
        baseMapper.deleteById(id);
    }

    private void validate(TeachingTask task) {
        if (task.getCourseTermId() == null || courseTermMapper.selectById(task.getCourseTermId()) == null)
            throw new BusinessException("开课记录不存在");
        if (task.getTeacherId() == null || teacherMapper.selectById(task.getTeacherId()) == null)
            throw new BusinessException("授课教师不存在");
    }
}
