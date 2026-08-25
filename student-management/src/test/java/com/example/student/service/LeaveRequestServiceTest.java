package com.example.student.service;

import com.example.student.entity.LeaveRequest;
import com.example.student.entity.Student;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.LeaveRequestMapper;
import com.example.student.mapper.StudentMapper;
import com.example.student.security.DataScopeFilter;
import com.example.student.security.DataScopeService;
import com.example.student.security.StudentDataScopeService;
import com.example.student.service.impl.LeaveRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 请假边界：本人/范围校验、时间合法性、状态流转、销假前置条件。 */
class LeaveRequestServiceTest {

    private LeaveRequestMapper mapper;
    private StudentMapper studentMapper;
    private DataScopeService dataScopeService;
    private StudentDataScopeService studentDataScopeService;
    private LeaveRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(LeaveRequestMapper.class);
        studentMapper = mock(StudentMapper.class);
        dataScopeService = mock(DataScopeService.class);
        studentDataScopeService = mock(StudentDataScopeService.class);
        service = new LeaveRequestServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "dataScopeService", dataScopeService);
        ReflectionTestUtils.setField(service, "studentDataScopeService", studentDataScopeService);

        Student student = new Student();
        student.setId(9L);
        student.setUserId(77L); // 登录用户 77 = 该学生本人
        when(studentMapper.selectById(9L)).thenReturn(student);
        when(mapper.insert(any(LeaveRequest.class))).thenReturn(1);
        when(mapper.updateById(any(LeaveRequest.class))).thenReturn(1);
    }

    /** 默认 stub：当前用户 88 无特殊范围，但对该生有访问权（供 submit/approve 正常路径用） */
    private DataScopeFilter defaultFilter() {
        DataScopeFilter f = new DataScopeFilter();
        f.setUserId(88L);
        when(dataScopeService.currentFilter()).thenReturn(f);
        return f;
    }

    private LeaveRequest validRequest() {
        LeaveRequest r = new LeaveRequest();
        r.setStudentId(9L);
        r.setStartTime(LocalDateTime.now().plusDays(1));
        r.setEndTime(LocalDateTime.now().plusDays(2));
        return r;
    }

    @Test
    void submitDefaultsToPending() {
        DataScopeFilter f = defaultFilter();
        f.setUserId(77L); // 本人提交
        service.submit(validRequest());
        ArgumentCaptor<LeaveRequest> cap = ArgumentCaptor.forClass(LeaveRequest.class);
        verify(mapper).insert(cap.capture());
        assertEquals("PENDING", cap.getValue().getStatus());
    }

    @Test
    void submitRejectsInvertedTimeRange() {
        DataScopeFilter f = defaultFilter();
        f.setUnrestricted(true); // 越过权限检查，专测时间校验
        LeaveRequest r = validRequest();
        r.setStartTime(LocalDateTime.now().plusDays(3));
        assertThrows(BusinessException.class, () -> service.submit(r));
    }

    @Test
    void submitDeniedForStrangerWithoutScope() {
        // 当前登录用户 88，与学生 userId 77 不匹配，且无数据范围
        DataScopeFilter f = new DataScopeFilter();
        f.setUserId(88L);
        when(dataScopeService.currentFilter()).thenReturn(f);
        when(dataScopeService.canAccessStudent(any())).thenReturn(false);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.submit(validRequest()));
    }

    @Test
    void approveOnlyFromPending() {
        LeaveRequest lr = validRequest();
        lr.setId(1L);
        lr.setStatus("APPROVED");
        when(mapper.selectById(1L)).thenReturn(lr);
        DataScopeFilter f = defaultFilter();
        f.setUnrestricted(true);
        when(dataScopeService.canAccessStudent(any())).thenReturn(true); // 权限通过，专测状态机
        assertThrows(BusinessException.class, () -> service.approve(1L, true, null));
    }

    @Test
    void approveDeniedWithoutStudentScope() {
        LeaveRequest lr = validRequest();
        lr.setId(2L);
        lr.setStatus("PENDING");
        when(mapper.selectById(2L)).thenReturn(lr);
        DataScopeFilter f = defaultFilter(); // currentFilter 已 stub
        when(dataScopeService.canAccessStudent(any())).thenReturn(false);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.approve(2L, true, null));
    }

    @Test
    void returnBackRequiresApproved() {
        LeaveRequest lr = validRequest();
        lr.setId(3L);
        lr.setStatus("PENDING");
        when(mapper.selectById(3L)).thenReturn(lr);
        DataScopeFilter f = defaultFilter();
        f.setUnrestricted(true);
        when(dataScopeService.canAccessStudent(any())).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.returnBack(3L));

        lr.setStatus("APPROVED");
        service.returnBack(3L);
        assertEquals("RETURNED", lr.getStatus());
        assertNotNull(lr.getReturnedAt());
    }
}
