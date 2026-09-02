package com.example.student.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.student.common.DistributedCacheService;
import com.example.student.entity.ScheduledTask;
import com.example.student.entity.ScheduledTaskLog;
import com.example.student.mapper.ScheduledTaskLogMapper;
import com.example.student.mapper.ScheduledTaskMapper;
import com.example.student.service.ScheduledTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.LocalDateTime;
import java.util.List;

/**
 * M1-1 动态定时任务调度器：启动时读取 sys_scheduled_task 中 enabled=1 的任务，
 * 按数据库中的 cron 注册执行。每次执行：跨实例互斥锁（防重复跑）→ 执行 → 写执行日志。
 * cron 变更需重启生效（简单实现；如需热更新可加轮询刷新，当前单机部署无此需求）。
 */
@Configuration
@EnableScheduling
public class ScheduledTaskScheduler implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskScheduler.class);

    private final ScheduledTaskMapper taskMapper;
    private final ScheduledTaskLogMapper taskLogMapper;
    private final ScheduledTaskService taskService;
    private final DistributedCacheService cache;
    private final com.example.student.service.channel.NotifyChannelDispatcher channelDispatcher;

    public ScheduledTaskScheduler(ScheduledTaskMapper taskMapper,
                                  ScheduledTaskLogMapper taskLogMapper,
                                  ScheduledTaskService taskService,
                                  DistributedCacheService cache,
                                  com.example.student.service.channel.NotifyChannelDispatcher channelDispatcher) {
        this.taskMapper = taskMapper;
        this.taskLogMapper = taskLogMapper;
        this.taskService = taskService;
        this.cache = cache;
        this.channelDispatcher = channelDispatcher;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        List<ScheduledTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<ScheduledTask>()
                .eq(ScheduledTask::getEnabled, 1));
        for (ScheduledTask task : tasks) {
            Runnable runnable = buildRunnable(task);
            registrar.addCronTask(runnable, task.getCron());
            log.info("已注册定时任务 [{}] {} cron={}", task.getTaskCode(), task.getTaskName(), task.getCron());
        }
    }

    private Runnable buildRunnable(ScheduledTask task) {
        return () -> {
            // ponytail: 跨实例互斥锁，TTL 10 分钟（覆盖任务最长预计耗时）；单实例部署实际无竞争
            if (!cache.putIfAbsent("sched:lock:" + task.getTaskCode(), "1", 600)) {
                log.warn("[{}] 上一次执行未结束或锁未释放，跳过本次", task.getTaskCode());
                return;
            }
            long start = System.currentTimeMillis();
            ScheduledTaskLog entry = new ScheduledTaskLog();
            entry.setTaskCode(task.getTaskCode());
            entry.setTaskName(task.getTaskName());
            try {
                dispatch(task.getTaskCode());
                entry.setStatus("SUCCESS");
            } catch (Throwable t) {
                log.error("[{}] 任务执行失败", task.getTaskCode(), t);
                entry.setStatus("FAILED");
                entry.setErrorMessage(String.valueOf(t.getMessage()).length() > 500
                        ? String.valueOf(t.getMessage()).substring(0, 500) : String.valueOf(t.getMessage()));
            } finally {
                entry.setCostMs(System.currentTimeMillis() - start);
                entry.setCreateTime(LocalDateTime.now());
                taskLogMapper.insert(entry);
                cache.evictByPrefix("sched:lock:" + task.getTaskCode());
            }
        };
    }

    private void dispatch(String taskCode) {
        switch (taskCode) {
            case "WARNING_SCAN" -> {
                int n = taskService.scanWarning();
                log.info("[WARNING_SCAN] 本次生成预警 {} 条", n);
            }
            case "TERM_AUTO_SWITCH" -> {
                int n = taskService.autoSwitchTerm();
                log.info("[TERM_AUTO_SWITCH] 切换学期 {} 次", n);
            }
            case "NOTIFY_CLEANUP" -> {
                int n = taskService.cleanupReadNotifications(30);
                log.info("[NOTIFY_CLEANUP] 清理已读通知 {} 条", n);
            }
            case "IMPORT_REAPER" -> {
                int n = taskService.reapTimedOutImportTasks(30);
                log.info("[IMPORT_REAPER] 回收超时导入任务 {} 个", n);
            }
            case "NOTIFY_CHANNEL_RETRY" -> {
                int n = channelDispatcher.retryFailed(3);
                log.info("[NOTIFY_CHANNEL_RETRY] 重试外发 {} 条", n);
            }
            default -> log.warn("[{}] 无对应任务实现，跳过", taskCode);
        }
    }
}