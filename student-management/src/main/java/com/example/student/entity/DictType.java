package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 字典类型（M2-1）：sys_dict_type */
@Data
@TableName("sys_dict_type")
public class DictType implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典类型标识（如 gender） */
    private String dictType;

    /** 字典名称（展示用） */
    private String dictName;

    /** 状态：0-停用 1-启用 */
    private Integer status;

    private String remark;

    private LocalDateTime createTime;
}
