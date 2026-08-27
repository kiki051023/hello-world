package com.example.student.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.student.mapper.ScoreMapper;
import com.example.student.security.StudentDataScopeService;
import com.example.student.service.impl.ScoreServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Map;

class ScoreServiceScopeTest {

    @Test
    void scorePageUsesTheResolvedStudentScope() {
        ScoreMapper mapper = mock(ScoreMapper.class);
        StudentDataScopeService scope = mock(StudentDataScopeService.class);
        when(scope.resolveStudentId(null)).thenReturn(11L);
        when(mapper.selectScorePage(any(Page.class), eq(11L), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(new Page<>());

        ScoreServiceImpl service = new ScoreServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "studentDataScopeService", scope);

        service.getScorePage(1L, 10L, null, null, null, null, null, null);

        verify(mapper).selectScorePage(any(Page.class), eq(11L), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void scoreExportUsesOneProjectionQueryInsteadOfLoadingRelatedTables() {
        ScoreMapper mapper = mock(ScoreMapper.class);
        when(mapper.selectScoreExport(null, null, 3L, null, null)).thenReturn(List.of(
                Map.of("studentNo", "2023001", "studentName", "张三", "courseName", "数学",
                        "score", new java.math.BigDecimal("90"))));

        ScoreServiceImpl service = new ScoreServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        service.exportScores(null, null, 3L, null);

        verify(mapper).selectScoreExport(null, null, 3L, null, null);
        verifyNoMoreInteractions(mapper);
    }
}
