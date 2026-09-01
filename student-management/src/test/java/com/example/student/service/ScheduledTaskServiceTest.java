package com.example.student.service;

import com.example.student.entity.AcademicTerm;
import com.example.student.entity.Notification;
import com.example.student.mapper.AcademicTermMapper;
import com.example.student.mapper.ImportTaskMapper;
import com.example.student.mapper.NotificationMapper;
import com.example.student.mapper.ScoreMapper;
import com.example.student.service.impl.ScheduledTaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** M1-1 定时任务实现单元测试：四个任务的调度语义（mock Mapper/Service）。 */
class ScheduledTaskServiceTest {

    private ScoreMapper scoreMapper;
    private NotificationMapper notificationMapper;
    private AcademicTermMapper academicTermMapper;
    private ImportTaskMapper importTaskMapper;
    private StudentWarningService studentWarningService;
    private ScheduledTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        scoreMapper = mock(ScoreMapper.class);
        notificationMapper = mock(NotificationMapper.class);
        academicTermMapper = mock(AcademicTermMapper.class);
        importTaskMapper = mock(ImportTaskMapper.class);
        studentWarningService = mock(StudentWarningService.class);
        service = new ScheduledTaskServiceImpl(scoreMapper, notificationMapper,
                academicTermMapper, importTaskMapper, studentWarningService);
    }

    @Test
    void scanWarningPassesFailCountsToRuleEngine() {
        Map<String, Object> row1 = new HashMap<>();
        row1.put("studentId", 1L);
        row1.put("failCount", 4);
        Map<String, Object> row2 = new HashMap<>();
        row2.put("studentId", 2L);
        row2.put("failCount", 1);
        when(scoreMapper.countFailCoursesPerStudent()).thenReturn(List.of(row1, row2));
        when(studentWarningService.generateByRule(anyString(), anyString(), anyInt(), anyMap())).thenReturn(1);

        int created = service.scanWarning();

        assertEquals(1, created);
        // 阈值 3 由规则引擎内部过滤：传全部统计过去，引擎只对 >=3 的生成
        verify(studentWarningService).generateByRule(eq("SCORE"), eq("FAIL_COURSE"), eq(3), argThat(map ->
                map.size() == 2 && map.get(1L).equals(4) && map.get(2L).equals(1)));
    }

    @Test
    void autoSwitchTermPicksEarliestStartedWhenNoCurrent() {
        when(academicTermMapper.selectCount(any())).thenReturn(0L);
        AcademicTerm next = new AcademicTerm();
        next.setId(7L);
        next.setStartDate(LocalDate.now().minusDays(5));
        when(academicTermMapper.selectOne(any())).thenReturn(next);

        int switched = service.autoSwitchTerm();

        assertEquals(1, switched);
        verify(academicTermMapper).resetCurrentFlag();
        verify(academicTermMapper).updateById(argThat(t -> t.getStatus() == 1));
    }

    @Test
    void autoSwitchTermKeepsCurrentWhenExists() {
        when(academicTermMapper.selectCount(any())).thenReturn(1L);

        int switched = service.autoSwitchTerm();

        assertEquals(0, switched);
        verify(academicTermMapper, never()).resetCurrentFlag();
        verify(academicTermMapper, never()).updateById(any());
    }

    @Test
    void cleanupReadNotificationsDeletesOldReadOnly() {
        when(notificationMapper.delete(any())).thenReturn(5);

        int n = service.cleanupReadNotifications(30);

        assertEquals(5, n);
        verify(notificationMapper).delete(argThat(wrapper ->
                wrapper.getExpression() != null)); // LambdaQueryWrapper 非空
    }

    @Test
    void reapTimedOutReusesMapperWithTimeoutMinutes() {
        when(importTaskMapper.reapTimedOut(30)).thenReturn(2);

        int n = service.reapTimedOutImportTasks(30);

        assertEquals(2, n);
        verify(importTaskMapper).reapTimedOut(30);
    }
}