package com.example.student.service;

import com.example.student.entity.ImportTask;

public interface ImportTaskService {
    ImportTask start(String businessType, String fileName, Long operatorId);
    void complete(Long taskId, int total, int success, int failure);
    void fail(Long taskId, String message);
}
