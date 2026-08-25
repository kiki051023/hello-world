package com.example.student.service;

import com.example.student.entity.ImportTask;
import com.example.student.mapper.ImportTaskMapper;
import com.example.student.service.impl.ImportTaskServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ImportTaskServiceTest {
    @Test
    void startsAndCompletesAnImportTask() {
        ImportTaskMapper mapper = mock(ImportTaskMapper.class);
        ImportTask task = new ImportTask(); task.setId(1L);
        when(mapper.selectById(1L)).thenReturn(task);
        ImportTaskService service = new ImportTaskServiceImpl(mapper);

        ImportTask started = service.start("student", "students.xlsx", 3L);
        verify(mapper).insert(started);
        service.complete(1L, 10, 8, 2);

        assertThat(task.getStatus()).isEqualTo("SUCCESS");
        assertThat(task.getFailureCount()).isEqualTo(2);
    }
}
