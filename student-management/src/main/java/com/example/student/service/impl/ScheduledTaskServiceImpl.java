package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.student.entity.AcademicTerm;
import com.example.student.entity.Notification;
import com.example.student.mapper.AcademicTermMapper;
import com.example.student.mapper.ImportTaskMapper;
import com.example.student.mapper.NotificationMapper;
import com.example.student.mapper.ScoreMapper;
import com.example.student.service.ScheduledTaskService;
import com.example.student.service.StudentWarningService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** M1-1 定时任务实现：四个首批任务，全部复用既有 Mapper/Service。 */
@Service
public class ScheduledTaskServiceImpl implements ScheduledTaskService {

    private final ScoreMapper scoreMapper;
    private final NotificationMapper notificationMapper;
    private final AcademicTermMapper academicTermMapper;
    private final ImportTaskMapper importTaskMapper;
    private final StudentWarningService studentWarningService;

    public ScheduledTaskServiceImpl(ScoreMapper scoreMapper,
                                    NotificationMapper notificationMapper,
                                    AcademicTermMapper academicTermMapper,
                                    ImportTaskMapper importTaskMapper,
                                    StudentWarningService studentWarningService) {
        this.scoreMapper = scoreMapper;
        this.notificationMapper = notificationMapper;
        this.academicTermMapper = academicTermMapper;
        this.importTaskMapper = importTaskMapper;
        this.studentWarningService = studentWarningService;
    }

    /** 学业预警扫描：统计每学生挂科课程数（每课取最高分 <60 计 1 门），≥3 门走 generateByRule 幂等生成+通知。 */
    @Override
    @Transactional
    public int scanWarning() {
        List<Map<String, Object>> failCounts = scoreMapper.countFailCoursesPerStudent();
        Map<Long, Integer> failByStudent = new HashMap<>();
        for (Map<String, Object> row : failCounts) {
            if (row == null || row.get("studentId") == null || row.get("failCount") == null) continue;
            failByStudent.put(Long.valueOf(String.valueOf(row.get("studentId"))),
                    Integer.valueOf(String.valueOf(row.get("failCount"))));
        }
        return studentWarningService.generateByRule("SCORE", "FAIL_COURSE", 3, failByStudent);
    }

    /** 学期自动切换：无当前学期时，取起止日期最早且已开始的学期置为当前。 */
    @Override
    @Transactional
    public int autoSwitchTerm() {
        Long currentCount = academicTermMapper.selectCount(new LambdaQueryWrapper<AcademicTerm>()
                .eq(AcademicTerm::getStatus, 1));
        if (currentCount == null || currentCount == 0) {
            AcademicTerm next = academicTermMapper.selectOne(new LambdaQueryWrapper<AcademicTerm>()
                    .le(AcademicTerm::getStartDate, LocalDateTime.now().toLocalDate())
                    .orderByAsc(AcademicTerm::getStartDate)
                    .last("LIMIT 1"));
            if (next == null) return 0;
            academicTermMapper.resetCurrentFlag();
            next.setStatus(1);
            academicTermMapper.updateById(next);
            return 1;
        }
        return 0;
    }

    /** 清理 N 天前已读通知。 */
    @Override
    @Transactional
    public int cleanupReadNotifications(int keepDays) {
        return notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getIsRead, 1)
                .lt(Notification::getCreateTime, LocalDateTime.now().minusDays(keepDays)));
    }

    /** 回收 RUNNING 且开始时间超时（分钟）的导入任务为 FAILED。 */
    @Override
    @Transactional
    public int reapTimedOutImportTasks(int timeoutMinutes) {
        return importTaskMapper.reapTimedOut(timeoutMinutes);
    }
}