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

    @ExcelProperty("错误信息")
    private String errorMessage;
}
