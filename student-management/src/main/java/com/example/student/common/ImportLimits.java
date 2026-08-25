package com.example.student.common;

import com.example.student.exception.BusinessException;

/** Limits synchronous Excel imports so parsing cannot exhaust application memory. */
public final class ImportLimits {

    public static final int MAX_SYNC_ROWS = 10_000;

    /** 同步导出行数上限；超过转异步任务 */
    public static final int MAX_EXPORT_ROWS = 10_000;

    private ImportLimits() {
    }

    public static void requireAllowedRowCount(int rowCount) {
        if (rowCount > MAX_SYNC_ROWS) {
            throw new BusinessException("导入行数不能超过" + MAX_SYNC_ROWS);
        }
    }
}
