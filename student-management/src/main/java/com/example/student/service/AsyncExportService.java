package com.example.student.service;

import com.example.student.common.ExportTask;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 异步导出：超过同步阈值的导出转任务表 + 线程池执行。
 * 任务幂等（taskNo 唯一）；失败记录 error_message；结果仅登记行数与对象 key。
 */
public interface AsyncExportService {

    /** 提交异步导出，立即返回 taskNo */
    ExportTask submit(String businessType, String paramsJson, Supplier<Integer> job);

    Map<String, Object> status(String taskNo);
}
