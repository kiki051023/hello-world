package com.example.student.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class TeacherExcel {

    @ExcelProperty("教师编号")
    private String teacherNo;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("性别")
    private String gender;

    @ExcelProperty("联系电话")
    private String phone;

    @ExcelProperty("错误信息")
    private String errorMessage;
}
