package com.example.student.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 学校 DTO */
@Data
public class SchoolDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String schoolCode;
    private String schoolName;
    private Integer status;
    private LocalDateTime createTime;
}