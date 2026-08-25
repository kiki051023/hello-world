package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("student")
public class Student implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String studentNo;

    private String name;

    private String gender;

    private LocalDate birthday;

    private String phone;

    private String address;

    private Long clazzId;

    private Long schoolId;

    private Long departmentId;

    private Long majorId;

    private Long userId;

    /** 学籍状态：READING在读/SUSPENDED休学/GRADUATED毕业/TRANSFERRED转出等 */
    private String studentStatus;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String clazzName;
}
