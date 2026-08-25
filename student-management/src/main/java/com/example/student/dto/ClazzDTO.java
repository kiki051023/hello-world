package com.example.student.dto;

import lombok.Data;

@Data
public class ClazzDTO {
    private Long id;
    private String clazzName;
    private String grade;
    private Long headTeacherId;
    private String headTeacherName;
}
