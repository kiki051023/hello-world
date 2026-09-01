package com.example.student.service;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * 定时任务服务（M1-1）：数据库驱动的调度框架。
 * 注册方式：ScheduledTaskScheduler 启动时扫描 sys_scheduled_task 中 enabled=1 的任务，
 * 为每个任务按 cron 注册 @Scheduled 方法；此处暴露每个任务的真实执行方法。
 * 执行约定：所有任务内部自行 try/catch，绝不抛出（调度线程吞异常会静默丢日志）。
 */
public interface ScheduledTaskService {

    /** 学业预警扫描：每日统计挂科课程数，超过阈值生成学业预警（幂等） */
    int scanWarning();

    /** 学期自动切换：按起止日期将当前学期置位（0 非当前 → 当前） */
    int autoSwitchTerm();

    /** 清理 N 天前已读通知 */
    int cleanupReadNotifications(int keepDays);

    /** 回收 RUNNING 且超时的导入任务 */
    int reapTimedOutImportTasks(int timeoutMinutes);
}