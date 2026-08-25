package com.example.student.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ScoreDTO {
    private Long id;
    private Long studentId;
    private Long courseId;
    private String studentNo;
    private String studentName;
    private String courseName;
    private BigDecimal score;
    private LocalDate examTime;
}
