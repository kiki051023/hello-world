package com.example.student.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class StudentDTO {
    private Long id;
    private String studentNo;
    private String name;
    private String gender;
    private LocalDate birthday;
    private String phone;
    private String address;
    private Long clazzId;
    private String clazzName;
}
