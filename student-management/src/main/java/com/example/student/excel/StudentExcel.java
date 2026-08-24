package com.example.student.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentExcel {

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("性别")
    private String gender;

    @ExcelProperty("出生日期")
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate birthday;

    @ExcelProperty("联系电话")
    private String phone;

    @ExcelProperty("班级")
    private String clazzName;

    @ExcelProperty("家庭住址")
    private String address;

    @ExcelProperty("错误信息")
    private String errorMessage;
}
