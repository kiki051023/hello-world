package com.example.student.audit;

import com.example.student.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

@Service
public class OperationLogService {
    private final OperationLogMapper mapper;

    public OperationLogService(OperationLogMapper mapper) {
        this.mapper = mapper;
    }

    public void record(OperationLog log) {
        try {
            mapper.insert(log);
        } catch (RuntimeException ignored) {
            // 审计存储故障不能改变业务请求结果；错误由运维日志和监控捕获。
        }
    }
}
