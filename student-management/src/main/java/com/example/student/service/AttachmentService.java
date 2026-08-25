package com.example.student.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.AttachmentObject;

/** 附件对象：登记元数据；下载经业务归属权限校验，仅返回对象 key 供网关解析。 */
public interface AttachmentService extends IService<AttachmentObject> {

    AttachmentObject register(AttachmentObject att);

    /** 权限校验后的下载许可：返回对象 key；无权抛 AccessDeniedException */
    String downloadKeyChecked(Long id);
}
