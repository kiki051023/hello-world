package com.example.student.service.impl;

import com.example.student.entity.ImportTask;
import com.example.student.mapper.ImportTaskMapper;
import com.example.student.service.ImportTaskService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ImportTaskServiceImpl implements ImportTaskService {
    private final ImportTaskMapper mapper;

    public ImportTaskServiceImpl(ImportTaskMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ImportTask start(String businessType, String fileName, Long operatorId) {
        ImportTask task = new ImportTask();
        task.setTaskNo(UUID.randomUUID().toString());
        task.setBusinessType(businessType);
        task.setFileName(fileName);
        task.setStatus("RUNNING");
        task.setOperatorId(operatorId);
        task.setStartedAt(LocalDateTime.now());
        mapper.insert(task);
        return task;
    }

    @Override
    public void complete(Long taskId, int total, int success, int failure) {
        ImportTask task = mapper.selectById(taskId);
        if (task == null) return;
        task.setStatus("SUCCESS");
        task.setTotalCount(total);
        task.setSuccessCount(success);
        task.setFailureCount(failure);
        task.setFinishedAt(LocalDateTime.now());
        mapper.updateById(task);
    }

    @Override
    public void fail(Long taskId, String message) {
        ImportTask task = mapper.selectById(taskId);
        if (task == null) return;
        task.setStatus("FAILED");
        task.setErrorMessage(message);
        task.setFinishedAt(LocalDateTime.now());
        mapper.updateById(task);
    }
}
