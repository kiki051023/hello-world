package com.example.student.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ScoreExcel {

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("姓名")
    private String studentName;

    @ExcelProperty("课程名称")
    private String courseName;

    @ExcelProperty("成绩")
    private BigDecimal score;

    @ExcelProperty("考试日期")
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate examTime;

    /** 可选：学期名称（如 2025-2026-1），匹配 academic_term.term_name */
    @ExcelProperty("学期")
    private String termName;

    @ExcelProperty("错误信息")
    private String errorMessage;
}
