package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 动态菜单（M4-6） */
@Data
@TableName("sys_menu")
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;
    private String title;
    private String path;
    private String icon;

    /** 可见角色码，逗号分隔（如 ADMIN,TEACHER） */
    private String roles;

    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
}
