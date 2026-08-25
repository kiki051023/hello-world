package com.example.student.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImportLimitsTest {

    @Test
    void rejectsFilesLargerThanTheSynchronousImportLimit() {
        assertThatThrownBy(() -> ImportLimits.requireAllowedRowCount(ImportLimits.MAX_SYNC_ROWS + 1))
                .isInstanceOf(com.example.student.exception.BusinessException.class)
                .hasMessageContaining("10000");
    }
}
