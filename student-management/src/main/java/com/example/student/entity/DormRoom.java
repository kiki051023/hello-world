package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

/** dorm_room（代码生成） */
@Data
@TableName("dorm_room")
public class DormRoom implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 楼栋 */
    private String building;

    /** 房间号 */
    private String roomNo;

    /** 容量 */
    private Integer capacity;

    /** 备注 */
    private String remark;

}
