package com.example.student.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.student.entity.CourseSelection;
import com.example.student.entity.CourseTerm;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.CourseSelectionMapper;
import com.example.student.mapper.CourseTermMapper;
import com.example.student.mapper.StudentMapper;
import com.example.student.security.DataScopeFilter;
import com.example.student.security.DataScopeService;
import com.example.student.security.StudentDataScopeService;
import com.example.student.service.impl.CourseSelectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 选课边界：容量、重复、代选权限。 */
class CourseSelectionServiceTest {

    private CourseSelectionMapper mapper;
    private CourseTermMapper courseTermMapper;
    private StudentMapper studentMapper;
    private DataScopeService dataScopeService;
    private StudentDataScopeService studentDataScopeService;
    private CourseSelectionServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(CourseSelectionMapper.class);
        courseTermMapper = mock(CourseTermMapper.class);
        studentMapper = mock(StudentMapper.class);
        dataScopeService = mock(DataScopeService.class);
        studentDataScopeService = mock(StudentDataScopeService.class);
        service = new CourseSelectionServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "courseTermMapper", courseTermMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "dataScopeService", dataScopeService);
        ReflectionTestUtils.setField(service, "studentDataScopeService", studentDataScopeService);
        when(studentDataScopeService.resolveStudentId(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private CourseTerm openTerm(int capacity, int selected) {
        CourseTerm term = new CourseTerm();
        term.setId(100L);
        term.setCourseId(20L);
        term.setCapacity(capacity);
        term.setStatus(1);
        when(courseTermMapper.selectById(100L)).thenReturn(term);
        when(mapper.selectCount(any())).thenReturn((long) selected);
        return term;
    }

    private CourseSelection selection(Long studentId) {
        CourseSelection s = new CourseSelection();
        s.setCourseTermId(100L);
        s.setStudentId(studentId);
        return s;
    }

    @Test
    void selectRejectsWhenFull() {
        openTerm(30, 30);
        assertThrows(BusinessException.class, () -> service.select(selection(10L)));
        verify(mapper, never()).insert(any());
    }

    @Test
    void selectRejectsDuplicate() {
        openTerm(30, 5);
        // 第二次 selectCount 返回 >0 表示已存在选课记录
        when(mapper.selectCount(any())).thenReturn(5L, 1L);
        assertThrows(BusinessException.class, () -> service.select(selection(10L)));
    }

    @Test
    void selectSucceedsWithCapacityAndDefaults() {
        openTerm(30, 29);
        when(mapper.selectCount(any())).thenReturn(29L, 0L);

        service.select(selection(10L));

        ArgumentCaptor<CourseSelection> captor = ArgumentCaptor.forClass(CourseSelection.class);
        verify(mapper).insert(captor.capture());
        assertEquals("NORMAL", captor.getValue().getSelectionType());
        assertEquals(1, captor.getValue().getStatus());
    }

    @Test
    void closedCourseTermRejected() {
        CourseTerm closed = openTerm(30, 0);
        closed.setStatus(0);
        assertThrows(BusinessException.class, () -> service.select(selection(10L)));
    }

    @Test
    void selectRejectsTimeConflict() {
        openTerm(30, 5);
        when(mapper.selectCount(any())).thenReturn(5L, 0L);
        // M4-2：时间槽冲突 → 拒绝
        when(mapper.countTimeConflict(10L, 100L)).thenReturn(1L);
        BusinessException e = assertThrows(BusinessException.class, () -> service.select(selection(10L)));
        assertEquals("该时间段与已选课程冲突", e.getMessage());
        verify(mapper, never()).insert(any());
    }

    @Test
    void cancelDeniedWhenNotSelfAndNoScope() {
        CourseSelection existing = selection(99L);
        existing.setId(7L);
        when(mapper.selectById(7L)).thenReturn(existing);
        DataScopeFilter filter = new DataScopeFilter();
        filter.setUserId(1L); // 当前登录用户不是该学生本人
        when(dataScopeService.currentFilter()).thenReturn(filter);
        when(dataScopeService.canAccessStudent(any())).thenReturn(false);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.cancel(7L));
        verify(mapper, never()).deleteById(any());
    }
}
