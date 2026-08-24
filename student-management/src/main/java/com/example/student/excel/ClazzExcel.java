package com.example.student.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ClazzExcel {

    @ExcelProperty("班级名称")
    private String clazzName;

    @ExcelProperty("年级")
    private String grade;

    @ExcelProperty("班主任")
    private String headTeacherName;

    @ExcelProperty("错误信息")
    private String errorMessage;
}
