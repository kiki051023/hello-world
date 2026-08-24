package com.example.student.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class CourseExcel {

    @ExcelProperty("课程编号")
    private String courseNo;

    @ExcelProperty("课程名称")
    private String courseName;

    @ExcelProperty("学分")
    private Integer credit;

    @ExcelProperty("任课教师")
    private String teacherName;

    @ExcelProperty("错误信息")
    private String errorMessage;
}
