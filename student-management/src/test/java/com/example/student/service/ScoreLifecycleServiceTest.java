package com.example.student.service;

import com.example.student.entity.Course;
import com.example.student.entity.Score;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.CourseMapper;
import com.example.student.mapper.ScoreChangeLogMapper;
import com.example.student.mapper.ScoreMapper;
import com.example.student.mapper.StudentMapper;
import com.example.student.security.DataScopeService;
import com.example.student.service.NotificationSender;
import com.example.student.service.ScoreLifecycleService;
import com.example.student.service.impl.ScoreLifecycleServiceImpl;
import com.example.student.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 成绩生命周期核心规则：状态流转、有效版本切换、补考重修独立记录、越权拒绝、变更历史。 */
class ScoreLifecycleServiceTest {

    private ScoreMapper scoreMapper;
    private ScoreChangeLogMapper changeLogMapper;
    private CourseMapper courseMapper;
    private StudentMapper studentMapper;
    private DataScopeService dataScopeService;
    private ScoreLifecycleServiceImpl service;

    @BeforeEach
    void setUp() {
        scoreMapper = mock(ScoreMapper.class);
        changeLogMapper = mock(ScoreChangeLogMapper.class);
        courseMapper = mock(CourseMapper.class);
        studentMapper = mock(StudentMapper.class);
        dataScopeService = mock(DataScopeService.class);
        service = new ScoreLifecycleServiceImpl();
        ReflectionTestUtils.setField(service, "scoreMapper", scoreMapper);
        ReflectionTestUtils.setField(service, "changeLogMapper", changeLogMapper);
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "dataScopeService", dataScopeService);
        ReflectionTestUtils.setField(service, "notificationSender", mock(NotificationSender.class));
        when(dataScopeService.canAccessCourse(any())).thenReturn(true);
        when(scoreMapper.updateById(any(Score.class))).thenReturn(1);
        when(changeLogMapper.insert(any())).thenReturn(1);
    }

    private Score approvedScore() {
        Score s = new Score();
        s.setId(1L);
        s.setStudentId(10L);
        s.setCourseId(20L);
        s.setScore(new BigDecimal("55"));
        s.setScoreStatus("APPROVED");
        s.setScoreType("REGULAR");
        s.setActive(1);
        s.setVersion(0);
        return s;
    }

    @Test
    void submitRejectsApprovedScores() {
        when(scoreMapper.selectById(1L)).thenReturn(approvedScore());
        assertThrows(BusinessException.class, () -> service.submit(1L));
    }

    @Test
    void approveSwitchesActiveFlagWithinOneTransaction() {
        Score submitted = approvedScore();
        submitted.setScoreStatus("SUBMITTED");
        submitted.setActive(0);
        when(scoreMapper.selectById(1L)).thenReturn(submitted);
        when(scoreMapper.selectList(any())).thenReturn(List.of());

        Score result = service.approve(1L);

        assertEquals("APPROVED", result.getScoreStatus());
        assertEquals(1, result.getActive());
        verify(scoreMapper).updateById(submitted);
    }

    @Test
    void makeupCreatesIndependentRecordLinkedToOriginal() {
        when(scoreMapper.selectById(1L)).thenReturn(approvedScore());

        Score makeup = service.createMakeup(1L, new BigDecimal("75"), 5L);

        assertEquals("MAKEUP", makeup.getScoreType());
        assertEquals("SUBMITTED", makeup.getScoreStatus());
        assertEquals(Integer.valueOf(0), makeup.getActive(), "补考记录审核前不得占用 active=1");
        assertEquals(Long.valueOf(5L), makeup.getCourseTermId());
        assertEquals(Long.valueOf(1L), makeup.getOriginalScoreId());
        verify(scoreMapper).insert(makeup);
    }

    @Test
    void makeupRejectsNonApprovedOriginal() {
        Score draft = approvedScore();
        draft.setScoreStatus("DRAFT");
        when(scoreMapper.selectById(1L)).thenReturn(draft);
        assertThrows(BusinessException.class, () -> service.createMakeup(1L, new BigDecimal("70"), null));
    }

    @Test
    void retakeKeepsOriginalUntouchedAndCreatesNewRecord() {
        when(scoreMapper.selectById(1L)).thenReturn(approvedScore());
        ArgumentCaptor<Score> captor = ArgumentCaptor.forClass(Score.class);

        service.createRetake(1L, new BigDecimal("80"), null, 9L);

        verify(scoreMapper).insert(captor.capture());
        Score retake = captor.getValue();
        assertEquals("RETAKE", retake.getScoreType());
        assertEquals(Long.valueOf(9L), retake.getCourseTermId());
        assertEquals(new BigDecimal("80"), retake.getScore());
        // 原成绩未被修改
        verify(scoreMapper, never()).updateById(any());
    }

    @Test
    void writeOperationDeniedWhenCourseOutOfScope() {
        when(scoreMapper.selectById(1L)).thenReturn(approvedScore());
        when(dataScopeService.canAccessCourse(any())).thenReturn(false);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.voidScore(1L, "test"));
    }

    @Test
    void changeValueRejectedForApprovedScores() {
        when(scoreMapper.selectById(1L)).thenReturn(approvedScore());
        assertThrows(BusinessException.class,
                () -> service.changeScoreValue(1L, new BigDecimal("90"), "try overwrite"));
    }

    @Test
    void changeValueAllowedForRejectedWithHistory() {
        Score rejected = approvedScore();
        rejected.setScoreStatus("REJECTED");
        when(scoreMapper.selectById(1L)).thenReturn(rejected);

        service.changeScoreValue(1L, new BigDecimal("66"), "修正录入错误");

        assertEquals(new BigDecimal("66"), rejected.getScore());
        verify(changeLogMapper).insert(any());
    }

    @Test
    void invalidScoreValueRejected() {
        Score draft = approvedScore();
        draft.setScoreStatus("DRAFT");
        draft.setActive(0);
        when(scoreMapper.selectById(1L)).thenReturn(draft);
        assertThrows(BusinessException.class, () -> service.changeScoreValue(1L, new BigDecimal("120"), null));
        assertThrows(BusinessException.class, () -> service.changeScoreValue(1L, null, null));
    }
}
