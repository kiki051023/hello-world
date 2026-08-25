package com.example.student.service.impl;

import com.example.student.entity.AttachmentObject;
import com.example.student.entity.PracticeAssignment;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.AttachmentObjectMapper;
import com.example.student.mapper.PracticeAssignmentMapper;
import com.example.student.security.DataScopeFilter;
import com.example.student.security.DataScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 附件下载权限：仅分配相关方（指导教师/学生本人/管理员）可取对象 key。 */
class AttachmentServiceTest {

    private AttachmentObjectMapper mapper;
    private PracticeAssignmentMapper assignmentMapper;
    private com.example.student.mapper.StudentMapper studentMapper;
    private DataScopeService dataScopeService;
    private AttachmentServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        mapper = mock(AttachmentObjectMapper.class);
        assignmentMapper = mock(PracticeAssignmentMapper.class);
        studentMapper = mock(com.example.student.mapper.StudentMapper.class);
        dataScopeService = mock(DataScopeService.class);
        service = new AttachmentServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "assignmentMapper", assignmentMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "dataScopeService", dataScopeService);
        when(mapper.insert(any(AttachmentObject.class))).thenAnswer(inv -> {
            AttachmentObject a = inv.getArgument(0);
            Field id = AttachmentObject.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(a, 55L);
            return 1;
        });
    }

    private AttachmentObject att(String businessType, Long businessId) {
        AttachmentObject a = new AttachmentObject();
        a.setId(9L);
        a.setObjectKey("obj-abc123");
        a.setOriginalName("week1.pdf");
        a.setBusinessType(businessType);
        a.setBusinessId(businessId);
        return a;
    }

    private PracticeAssignment assignment(Long instructorId, Long studentId) {
        PracticeAssignment p = new PracticeAssignment();
        p.setId(businessIdOf(instructorId, studentId));
        p.setInstructorTeacherId(instructorId);
        p.setStudentId(studentId);
        return p;
    }

    private Long businessIdOf(Long i, Long s) { return 70L; }

    @Test
    void registerGeneratesObjectKey() {
        when(mapper.selectById(9L)).thenReturn(att("PRACTICE_LOG", 70L));
        // register path
        AttachmentObject in = new AttachmentObject();
        in.setOriginalName("a.pdf");
        in.setBusinessType("PRACTICE_LOG");
        AttachmentObject saved = service.register(in);
        assertNotNull(saved.getObjectKey());
        assertFalse(saved.getObjectKey().isBlank());
    }

    @Test
    void downloadAllowedForInstructor() {
        PracticeAssignment pa = assignment(5L, 9L);
        when(mapper.selectById(9L)).thenReturn(att("PRACTICE_LOG", 70L));
        when(assignmentMapper.selectById(70L)).thenReturn(pa);
        DataScopeFilter f = new DataScopeFilter();
        f.setTeacherId(5L); // 指导教师本人
        when(dataScopeService.currentFilter()).thenReturn(f);

        assertEquals("obj-abc123", service.downloadKeyChecked(9L));
    }

    @Test
    void downloadDeniedForUnrelatedUser() {
        PracticeAssignment pa = assignment(5L, 9L);
        when(mapper.selectById(9L)).thenReturn(att("PRACTICE_LOG", 70L));
        when(assignmentMapper.selectById(70L)).thenReturn(pa);
        DataScopeFilter f = new DataScopeFilter();
        f.setUserId(999L); // 无关用户
        when(dataScopeService.currentFilter()).thenReturn(f);
        Mockito.reset(dataScopeService);
        when(dataScopeService.currentFilter()).thenReturn(f);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.downloadKeyChecked(9L));
    }
}
