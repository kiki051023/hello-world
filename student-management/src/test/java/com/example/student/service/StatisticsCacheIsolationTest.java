package com.example.student.service;

import com.example.student.mapper.ClazzMapper;
import com.example.student.mapper.CourseMapper;
import com.example.student.mapper.ScoreMapper;
import com.example.student.mapper.StudentMapper;
import com.example.student.mapper.TeacherMapper;
import com.example.student.security.DataScopeFilter;
import com.example.student.security.DataScopeService;
import com.example.student.security.StudentDataScopeService;
import com.example.student.service.impl.StatisticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 统计缓存隔离：不同 DataScope 不得命中同一缓存键；同 scope 命中缓存只查一次。 */
class StatisticsCacheIsolationTest {

    private StudentMapper studentMapper;
    private ScoreMapper scoreMapper;
    private DataScopeService dataScopeService;
    private StatisticsServiceImpl service;

    @BeforeEach
    void setUp() {
        studentMapper = mock(StudentMapper.class);
        scoreMapper = mock(ScoreMapper.class);
        dataScopeService = mock(DataScopeService.class);
        service = new StatisticsServiceImpl();
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "clazzMapper", mock(ClazzMapper.class));
        ReflectionTestUtils.setField(service, "courseMapper", mock(CourseMapper.class));
        ReflectionTestUtils.setField(service, "teacherMapper", mock(TeacherMapper.class));
        ReflectionTestUtils.setField(service, "scoreMapper", scoreMapper);
        ReflectionTestUtils.setField(service, "studentDataScopeService",
                mock(StudentDataScopeService.class));
        ReflectionTestUtils.setField(service, "dataScopeService", dataScopeService);
        ReflectionTestUtils.setField(service, "distributedCache", newStatefulCacheFake());

        Map<String, BigDecimal> stats = new HashMap<>();
        stats.put("avg", BigDecimal.ZERO);
        stats.put("passRate", BigDecimal.ZERO);
        stats.put("max", BigDecimal.ZERO);
        stats.put("min", BigDecimal.ZERO);
        when(scoreMapper.selectScoreStats(any(), any(), any(), any(), any())).thenReturn(stats);
        Map<String, Object> pf = new HashMap<>();
        pf.put("passCount", 0);
        pf.put("failCount", 0);
        when(scoreMapper.selectPassFail(any(), any())).thenReturn(pf);
        when(scoreMapper.selectScoreDistribution(any(), any())).thenReturn(java.util.List.of());
    }

    /** 按调用次序返回预设 filter（Mockito thenReturn 链在多次 stub 时会被覆盖，改用队列） */
    private void queueFilters(DataScopeFilter... filters) {
        Deque<DataScopeFilter> queue = new ArrayDeque<>(java.util.Arrays.asList(filters));
        when(dataScopeService.currentFilter()).thenAnswer(inv -> {
            DataScopeFilter next = queue.poll();
            return next != null ? next : filters[filters.length - 1];
        });
    }

    private DataScopeFilter filter(Long userId, boolean unrestricted) {
        DataScopeFilter f = new DataScopeFilter();
        f.setUserId(userId);
        f.setUnrestricted(unrestricted);
        return f;
    }

    @Test
    void sameScopeHitsCacheOnlyOnce() {
        queueFilters(filter(1L, true));

        service.getOverview();
        service.getOverview();
        service.getOverview();

        verify(studentMapper, times(1)).countScoped(any());
    }

    @Test
    void differentScopesDoNotShareCache() {
        // admin → counselor → admin：第 2 次必须重查（scope 不同），第 3 次命中缓存
        queueFilters(
                filter(1L, true),
                filter(2L, false),
                filter(1L, true)
        );

        service.getOverview(); // admin
        service.getOverview(); // counselor — 不同 scope，必须重查
        verify(studentMapper, times(2)).countScoped(any());

        service.getOverview(); // admin again — TTL 内缓存仍在，不查库
        verify(studentMapper, times(2)).countScoped(any()); // 仍 2 次：admin 结果来自缓存
    }

    /** 有状态缓存 fake：模拟 DistributedCacheService 的本地降级路径（TTL 内命中返回缓存值） */
    private com.example.student.common.DistributedCacheService newStatefulCacheFake() {
        return new com.example.student.common.DistributedCacheService(null) {
            private final java.util.Map<String, Object> store = new java.util.concurrent.ConcurrentHashMap<>();

            @Override
            @SuppressWarnings("unchecked")
            public <T> T getOrLoad(String key, long ttlSeconds, java.util.function.Function<String, T> loader) {
                return (T) store.computeIfAbsent(key, loader);
            }
        };
    }
}
