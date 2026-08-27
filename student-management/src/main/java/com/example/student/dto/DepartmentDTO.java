package com.example.student.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 系部 DTO */
@Data
public class DepartmentDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long schoolId;
    private String schoolName;
    private String departmentCode;
    private String departmentName;
    private Long leaderUserId;
    private Integer status;
    private LocalDateTime createTime;
}