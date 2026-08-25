package com.example.student.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.TeachingTask;

import java.util.Map;

public interface TeachingTaskService extends IService<TeachingTask> {

    Map<String, Object> getPage(long current, long size, Long teacherId, Long termId, Long clazzId);

    TeachingTask getByIdChecked(Long id);

    void addTeachingTask(TeachingTask task);

    void updateTeachingTask(TeachingTask task);

    void deleteTeachingTask(Long id);
}
