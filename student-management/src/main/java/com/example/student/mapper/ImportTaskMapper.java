package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.student.entity.ImportTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImportTaskMapper extends BaseMapper<ImportTask> {

    /** 回收 RUNNING 且开始时间超过 timeoutMinutes 的导入任务为 FAILED（超时回收，返回回收数） */
    @org.apache.ibatis.annotations.Update("""
            UPDATE import_task SET status = 'FAILED', error_message = '导入超时被系统回收',
                   finished_at = NOW()
            WHERE status = 'RUNNING' AND started_at < DATE_SUB(NOW(), INTERVAL #{timeoutMinutes} MINUTE)
            """)
    int reapTimedOut(@org.apache.ibatis.annotations.Param("timeoutMinutes") int timeoutMinutes);
}
