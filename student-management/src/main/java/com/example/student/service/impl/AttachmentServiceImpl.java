package com.example.student.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.AttachmentObject;
import com.example.student.entity.PracticeAssignment;
import com.example.student.entity.Student;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.AttachmentObjectMapper;
import com.example.student.mapper.PracticeAssignmentMapper;
import com.example.student.mapper.StudentMapper;
import com.example.student.security.DataScopeFilter;
import com.example.student.security.DataScopeService;
import com.example.student.service.AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

/** 附件对象：只存对象 key，不落物理路径；下载经业务归属权限校验。 */
@Service
public class AttachmentServiceImpl extends ServiceImpl<AttachmentObjectMapper, AttachmentObject>
        implements AttachmentService {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    @Lazy
    private PracticeAssignmentMapper assignmentMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private DataScopeService dataScopeService;

    @Override
    public AttachmentObject register(AttachmentObject att) {
        if (att.getOriginalName() == null || att.getOriginalName().isBlank())
            throw new BusinessException("原始文件名不能为空");
        byte[] buf = new byte[16];
        RANDOM.nextBytes(buf);
        att.setObjectKey(Base64.getUrlEncoder().withoutPadding().encodeToString(buf));
        att.setId(null);
        save(att);
        return att;
    }

    @Override
    public String downloadKeyChecked(Long id) {
        AttachmentObject a = getById(id);
        if (a == null) throw new BusinessException("附件不存在");
        DataScopeFilter f = dataScopeService.currentFilter();
        boolean allowed = f.isUnrestricted();

        // PRACTICE_LOG 附件：指导教师或学生本人
        if (!allowed && "PRACTICE_LOG".equals(a.getBusinessType()) && a.getBusinessId() != null) {
            PracticeAssignment pa = assignmentMapper.selectById(a.getBusinessId());
            if (pa != null) {
                if (f.getTeacherId() != null && f.getTeacherId().equals(pa.getInstructorTeacherId())) {
                    allowed = true;
                } else {
                    Student s = studentMapper.selectById(pa.getStudentId());
                    allowed = s != null && s.getUserId() != null && s.getUserId().equals(f.getUserId());
                }
            }
        }
        if (!allowed) throw new AccessDeniedException("无权下载该附件");
        return a.getObjectKey();
    }
}
