package com.example.student.dto;

import lombok.Data;

@Data
public class CourseDTO {
    private Long id;
    private String courseNo;
    private String courseName;
    private Integer credit;
    private Long teacherId;
    private String teacherName;
}
