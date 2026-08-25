package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 附件对象：只存对象 key，下载经权限校验后由服务端解析，不暴露物理路径。 */
@Data
@TableName("attachment_object")
public class AttachmentObject implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String objectKey;

    private String originalName;

    private String mimeType;

    private Long sizeBytes;

    /** PRACTICE_LOG/CERTIFICATE/OTHER */
    private String businessType;

    private Long businessId;

    private Long uploaderId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
