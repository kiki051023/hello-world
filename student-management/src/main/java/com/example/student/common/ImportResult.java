package com.example.student.common;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ImportResult<T> {

    /** 文件总行数（不含表头） */
    private int total = 0;

    /** 成功写入行数 */
    private int successCount = 0;

    /** 失败行明细（原始数据 + 错误信息），便于前端导出错误 Excel */
    private List<T> failRows = new ArrayList<>();
}
