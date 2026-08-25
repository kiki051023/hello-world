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

class ScoreServiceScopeTest {

    @Test
    void scorePageUsesTheResolvedStudentScope() {
        ScoreMapper mapper = mock(ScoreMapper.class);
        StudentDataScopeService scope = mock(StudentDataScopeService.class);
        when(scope.resolveStudentId(null)).thenReturn(11L);
        when(mapper.selectScorePage(any(Page.class), eq(11L), isNull(), isNull()))
                .thenReturn(new Page<>());

        ScoreServiceImpl service = new ScoreServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "studentDataScopeService", scope);

        service.getScorePage(1L, 10L, null, null, null);

        verify(mapper).selectScorePage(any(Page.class), eq(11L), isNull(), isNull());
    }
}
