package com.example.student.service;

import com.example.student.entity.Department;
import com.example.student.entity.Major;
import com.example.student.entity.School;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.DepartmentMapper;
import com.example.student.mapper.MajorMapper;
import com.example.student.mapper.ClazzMapper;
import com.example.student.mapper.StudentMapper;
import com.example.student.mapper.TrainingPlanMapper;
import com.example.student.mapper.SchoolMapper;
import com.example.student.service.impl.DepartmentServiceImpl;
import com.example.student.service.impl.MajorServiceImpl;
import com.example.student.service.impl.SchoolServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** P2-C 组织架构：新增时的必填/编码唯一校验（规则在 Service 层） */
class OrgManageServiceTest {

    // ---------- School ----------

    private SchoolMapper schoolMapper;
    private SchoolServiceImpl schoolService;

    @BeforeEach
    void setUpSchool() {
        schoolMapper = mock(SchoolMapper.class);
        schoolService = new SchoolServiceImpl();
        ReflectionTestUtils.setField(schoolService, "baseMapper", schoolMapper);
        when(schoolMapper.insert(any(School.class))).thenReturn(1);
        when(schoolMapper.selectCount(any())).thenReturn(0L);
    }

    @Test
    void addSchoolRequiresCodeAndName() {
        School s = new School();
        s.setSchoolName("测试学校");
        assertThrows(BusinessException.class, () -> schoolService.addSchool(s));
        s.setSchoolCode("X001");
        s.setSchoolName(null);
        assertThrows(BusinessException.class, () -> schoolService.addSchool(s));
    }

    @Test
    void addSchoolRejectsDuplicateCode() {
        when(schoolMapper.selectCount(any())).thenReturn(1L);
        School s = new School();
        s.setSchoolCode("X001");
        s.setSchoolName("测试学校");
        assertThrows(BusinessException.class, () -> schoolService.addSchool(s));
    }

    @Test
    void addSchoolDefaultsStatusToOne() {
        School s = new School();
        s.setSchoolCode("X001");
        s.setSchoolName("测试学校");
        schoolService.addSchool(s);
        assertEquals(Integer.valueOf(1), s.getStatus());
        verify(schoolMapper).insert(s);
    }

    // ---------- Department ----------

    private DepartmentMapper departmentMapper;
    private DepartmentServiceImpl departmentService;

    @Test
    void addDepartmentRequiresSchoolAndCodeAndName() {
        departmentMapper = mock(DepartmentMapper.class);
        departmentService = new DepartmentServiceImpl();
        ReflectionTestUtils.setField(departmentService, "baseMapper", departmentMapper);
        when(departmentMapper.selectCount(any())).thenReturn(0L);
        when(departmentMapper.insert(any(Department.class))).thenReturn(1);

        Department d = new Department();
        d.setSchoolId(1L);
        d.setDepartmentCode("D01");
        assertThrows(BusinessException.class, () -> departmentService.addDepartment(d));

        d.setDepartmentName("信息工程系");
        departmentService.addDepartment(d);
        assertEquals(Integer.valueOf(1), d.getStatus());
    }

    @Test
    void addDepartmentRejectsDuplicateCodeInSameSchool() {
        departmentMapper = mock(DepartmentMapper.class);
        departmentService = new DepartmentServiceImpl();
        ReflectionTestUtils.setField(departmentService, "baseMapper", departmentMapper);
        when(departmentMapper.selectCount(any())).thenReturn(1L);

        Department d = new Department();
        d.setSchoolId(1L);
        d.setDepartmentCode("D01");
        d.setDepartmentName("信息工程系");
        assertThrows(BusinessException.class, () -> departmentService.addDepartment(d));
    }

    // ---------- Major ----------

    @Test
    void addMajorRequiresDepartmentAndCodeAndName() {
        MajorMapper majorMapper = mock(MajorMapper.class);
        MajorServiceImpl majorService = new MajorServiceImpl();
        ReflectionTestUtils.setField(majorService, "baseMapper", majorMapper);
        when(majorMapper.selectCount(any())).thenReturn(0L);
        when(majorMapper.insert(any(Major.class))).thenReturn(1);

        Major m = new Major();
        m.setDepartmentId(1L);
        m.setMajorCode("M01");
        assertThrows(BusinessException.class, () -> majorService.addMajor(m));

        m.setMajorName("软件技术");
        majorService.addMajor(m);
        assertEquals(Integer.valueOf(1), m.getStatus());
    }

    @Test
    void addMajorRejectsDuplicateCodeInSameDepartment() {
        MajorMapper majorMapper = mock(MajorMapper.class);
        MajorServiceImpl majorService = new MajorServiceImpl();
        ReflectionTestUtils.setField(majorService, "baseMapper", majorMapper);
        when(majorMapper.selectCount(any())).thenReturn(1L);

        Major m = new Major();
        m.setDepartmentId(1L);
        m.setMajorCode("M01");
        m.setMajorName("软件技术");
        assertThrows(BusinessException.class, () -> majorService.addMajor(m));
    }

    // ---------- 删除保护 ----------

    @Test
    void deleteSchoolBlockedWhenDepartmentsExist() {
        SchoolServiceImpl svc = new SchoolServiceImpl();
        ReflectionTestUtils.setField(svc, "baseMapper", schoolMapper);
        DepartmentMapper deptMapper = mock(DepartmentMapper.class);
        ReflectionTestUtils.setField(svc, "departmentMapper", deptMapper);
        when(deptMapper.selectCount(any())).thenReturn(2L);
        when(schoolMapper.selectById(1L)).thenReturn(new School());

        BusinessException ex = assertThrows(BusinessException.class, () -> svc.deleteSchool(1L));
        assertTrue(ex.getMessage().contains("2 个系部"));
        verify(schoolMapper, never()).deleteById(any());
    }

    @Test
    void deleteSchoolSucceedsWhenNoDepartments() {
        SchoolServiceImpl svc = new SchoolServiceImpl();
        ReflectionTestUtils.setField(svc, "baseMapper", schoolMapper);
        DepartmentMapper deptMapper = mock(DepartmentMapper.class);
        ReflectionTestUtils.setField(svc, "departmentMapper", deptMapper);
        when(deptMapper.selectCount(any())).thenReturn(0L);
        when(schoolMapper.selectById(1L)).thenReturn(new School());
        when(schoolMapper.deleteById(1L)).thenReturn(1);

        svc.deleteSchool(1L);
        verify(schoolMapper).deleteById(1L);
    }

    @Test
    void deleteDepartmentBlockedWhenMajorsExist() {
        DepartmentServiceImpl svc = new DepartmentServiceImpl();
        DepartmentMapper mapper = mock(DepartmentMapper.class);
        ReflectionTestUtils.setField(svc, "baseMapper", mapper);
        when(mapper.selectById(1L)).thenReturn(new Department());
        MajorMapper majorMapper = mock(MajorMapper.class);
        when(majorMapper.selectCount(any())).thenReturn(3L);
        ReflectionTestUtils.setField(svc, "majorMapper", majorMapper);
        ReflectionTestUtils.setField(svc, "clazzMapper", mock(ClazzMapper.class));
        ReflectionTestUtils.setField(svc, "studentMapper", mock(StudentMapper.class));

        BusinessException ex = assertThrows(BusinessException.class, () -> svc.deleteDepartment(1L));
        assertTrue(ex.getMessage().contains("3 个专业"));
    }

    @Test
    void deleteDepartmentSucceedsWhenUnreferenced() {
        DepartmentServiceImpl svc = new DepartmentServiceImpl();
        DepartmentMapper mapper = mock(DepartmentMapper.class);
        ReflectionTestUtils.setField(svc, "baseMapper", mapper);
        when(mapper.selectById(1L)).thenReturn(new Department());
        MajorMapper majorMapper = mock(MajorMapper.class);
        when(majorMapper.selectCount(any())).thenReturn(0L);
        ReflectionTestUtils.setField(svc, "majorMapper", majorMapper);
        ClazzMapper clazzMapper = mock(ClazzMapper.class);
        when(clazzMapper.selectCount(any())).thenReturn(0L);
        ReflectionTestUtils.setField(svc, "clazzMapper", clazzMapper);
        StudentMapper studentMapper = mock(StudentMapper.class);
        when(studentMapper.selectCount(any())).thenReturn(0L);
        ReflectionTestUtils.setField(svc, "studentMapper", studentMapper);
        when(mapper.deleteById(1L)).thenReturn(1);

        svc.deleteDepartment(1L);
        verify(mapper).deleteById(1L);
    }

    @Test
    void deleteMajorBlockedWhenStudentsExist() {
        MajorServiceImpl svc = new MajorServiceImpl();
        MajorMapper mapper = mock(MajorMapper.class);
        ReflectionTestUtils.setField(svc, "baseMapper", mapper);
        when(mapper.selectById(1L)).thenReturn(new Major());
        ClazzMapper clazzMapper = mock(ClazzMapper.class);
        when(clazzMapper.selectCount(any())).thenReturn(0L);
        ReflectionTestUtils.setField(svc, "clazzMapper", clazzMapper);
        StudentMapper studentMapper = mock(StudentMapper.class);
        when(studentMapper.selectCount(any())).thenReturn(5L);
        ReflectionTestUtils.setField(svc, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(svc, "trainingPlanMapper", mock(TrainingPlanMapper.class));

        BusinessException ex = assertThrows(BusinessException.class, () -> svc.deleteMajor(1L));
        assertTrue(ex.getMessage().contains("5 名学生"));
    }

    @Test
    void deleteMajorSucceedsWhenUnreferenced() {
        MajorServiceImpl svc = new MajorServiceImpl();
        MajorMapper mapper = mock(MajorMapper.class);
        ReflectionTestUtils.setField(svc, "baseMapper", mapper);
        when(mapper.selectById(1L)).thenReturn(new Major());
        ClazzMapper clazzMapper = mock(ClazzMapper.class);
        when(clazzMapper.selectCount(any())).thenReturn(0L);
        ReflectionTestUtils.setField(svc, "clazzMapper", clazzMapper);
        StudentMapper studentMapper = mock(StudentMapper.class);
        when(studentMapper.selectCount(any())).thenReturn(0L);
        ReflectionTestUtils.setField(svc, "studentMapper", studentMapper);
        TrainingPlanMapper planMapper = mock(TrainingPlanMapper.class);
        when(planMapper.selectCount(any())).thenReturn(0L);
        ReflectionTestUtils.setField(svc, "trainingPlanMapper", planMapper);
        when(mapper.deleteById(1L)).thenReturn(1);

        svc.deleteMajor(1L);
        verify(mapper).deleteById(1L);
    }
}