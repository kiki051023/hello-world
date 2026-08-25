package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.student.common.ApiExchangeAudit;
import com.example.student.common.ApiIdempotency;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApiIdempotencyMapper extends BaseMapper<ApiIdempotency> {

    /** 交换审计直接 SQL 写入（避免与审计表 mapper 循环依赖） */
    @Insert("""
            INSERT INTO api_exchange_audit
              (client_id, api_version, request_method, request_path,
               idempotency_key, http_status, rejected_reason, request_id)
            VALUES
              (#{e.clientId}, #{e.apiVersion}, #{e.requestMethod}, #{e.requestPath},
               #{e.idempotencyKey}, #{e.httpStatus}, #{e.rejectedReason}, #{e.requestId})
            """)
    int insertAudit(@org.apache.ibatis.annotations.Param("e") ApiExchangeAudit e);
}
