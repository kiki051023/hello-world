package com.example.student.service;

import com.example.student.mapper.ScoreMapper;
import com.example.student.security.StudentDataScopeService;
import com.example.student.service.impl.StatisticsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class StatisticsServiceScopeTest {

    @Test
    void studentRadarUsesTheResolvedStudentId() {
        ScoreMapper mapper = mock(ScoreMapper.class);
        StudentDataScopeService scope = mock(StudentDataScopeService.class);
        when(scope.resolveStudentId(99L)).thenReturn(11L);
        when(mapper.selectStudentCourseScores(11L)).thenReturn(List.of(
                Map.of("courseName", "数学", "score", new java.math.BigDecimal("90"), "studentName", "学生")));

        StatisticsServiceImpl service = new StatisticsServiceImpl();
        ReflectionTestUtils.setField(service, "scoreMapper", mapper);
        ReflectionTestUtils.setField(service, "studentDataScopeService", scope);
        com.example.student.common.DistributedCacheService distributedCache =
                mock(com.example.student.common.DistributedCacheService.class);
        when(distributedCache.getOrLoad(any(), anyLong(), any()))
                .thenAnswer(inv -> ((java.util.function.Function<String, ?>) inv.getArgument(2))
                        .apply(inv.getArgument(0)));
        ReflectionTestUtils.setField(service, "distributedCache", distributedCache);

        service.getStudentRadar(99L);

        verify(mapper).selectStudentCourseScores(eq(11L));
        verify(mapper, never()).selectStudentCourseScores(eq(99L));
    }
}
