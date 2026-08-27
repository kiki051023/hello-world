package com.example.student.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 专业 DTO */
@Data
public class MajorDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long departmentId;
    private String departmentName;
    private String majorCode;
    private String majorName;
    private String educationLevel;
    private BigDecimal durationYears;
    private Integer status;
    private LocalDateTime createTime;
}