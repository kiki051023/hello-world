package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 字典数据（M2-1）：sys_dict_data */
@Data
@TableName("sys_dict_data")
public class DictData implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典类型标识 */
    private String dictType;

    /** 字典值（存储值） */
    private String dictValue;

    /** 字典标签（显示值） */
    private String dictLabel;

    /** 排序（升序） */
    private Integer sort;

    /** 状态：0-停用 1-启用 */
    private Integer status;

    private String remark;

    private LocalDateTime createTime;
}
