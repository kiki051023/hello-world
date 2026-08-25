package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.common.ExportTask;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.ExportTaskMapper;
import com.example.student.service.AsyncExportService;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@Service
public class AsyncExportServiceImpl extends ServiceImpl<ExportTaskMapper, ExportTask>
        implements AsyncExportService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "async-export");
        t.setDaemon(true);
        return t;
    });

    @Override
    public ExportTask submit(String businessType, String paramsJson, Supplier<Integer> job) {
        byte[] buf = new byte[12];
        RANDOM.nextBytes(buf);
        String taskNo = "EXP-" + Base64.getUrlEncoder().withoutPadding().encodeToString(buf);

        ExportTask task = new ExportTask();
        task.setTaskNo(taskNo);
        task.setBusinessType(businessType);
        task.setParamsJson(paramsJson);
        task.setStatus("PENDING");
        save(task);

        executor.submit(() -> run(task.getId(), job));
        return task;
    }

    void run(Long taskId, Supplier<Integer> job) {
        ExportTask task = getById(taskId);
        if (task == null || !"PENDING".equals(task.getStatus())) return;
        task.setStatus("RUNNING");
        updateById(task);
        try {
            int rows = job.get();
            task.setStatus("SUCCESS");
            task.setTotalRows(rows);
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setErrorMessage(truncate(e.getMessage()));
        }
        updateById(task);
    }

    @Override
    public Map<String, Object> status(String taskNo) {
        ExportTask task = getOne(new LambdaQueryWrapper<ExportTask>()
                .eq(ExportTask::getTaskNo, taskNo));
        if (task == null) throw new BusinessException("导出任务不存在");
        Map<String, Object> result = new HashMap<>();
        result.put("taskNo", task.getTaskNo());
        result.put("status", task.getStatus());
        result.put("totalRows", task.getTotalRows());
        result.put("errorMessage", task.getErrorMessage());
        return result;
    }

    private String truncate(String msg) {
        if (msg == null) return "export failed";
        return msg.length() > 480 ? msg.substring(0, 480) : msg;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
