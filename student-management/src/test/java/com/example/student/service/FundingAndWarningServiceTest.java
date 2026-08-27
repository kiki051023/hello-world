package com.example.student.service;

import com.example.student.entity.HardshipFunding;
import com.example.student.entity.Student;
import com.example.student.entity.StudentWarning;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.HardshipFundingMapper;
import com.example.student.mapper.StudentMapper;
import com.example.student.mapper.StudentWarningMapper;
import com.example.student.security.DataScopeFilter;
import com.example.student.security.DataScopeService;
import com.example.student.service.NotificationSender;
import com.example.student.service.impl.HardshipFundingServiceImpl;
import com.example.student.service.impl.StudentWarningServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 资助脱敏 + 预警规则引擎幂等性。 */
class FundingAndWarningServiceTest {

    // ---------- 资助 ----------

    @org.junit.jupiter.api.Nested
    class Funding {

        private HardshipFundingMapper mapper;
        private StudentMapper studentMapper;
        private DataScopeService dataScopeService;
        private HardshipFundingServiceImpl service;

        @BeforeEach
        void setUp() {
            mapper = mock(HardshipFundingMapper.class);
            studentMapper = mock(StudentMapper.class);
            dataScopeService = mock(DataScopeService.class);
            service = new HardshipFundingServiceImpl();
            ReflectionTestUtils.setField(service, "baseMapper", mapper);
            ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
            ReflectionTestUtils.setField(service, "dataScopeService", dataScopeService);

            Student s = new Student();
            s.setId(9L);
            when(studentMapper.selectById(9L)).thenReturn(s);
        }

        private HardshipFunding row() {
            HardshipFunding f = new HardshipFunding();
            f.setId(100L);
            f.setStudentId(9L);
            f.setSensitiveNote("家庭变故细节");
            return f;
        }

        @Test
        void sensitiveNoteMaskedForScopedNonUnrestrictedRole() {
            // 辅导员：canAccessStudent=true 但非 unrestricted → 脱敏
            DataScopeFilter filter = new DataScopeFilter();
            when(dataScopeService.currentFilter()).thenReturn(filter);
            when(dataScopeService.canAccessStudent(any())).thenReturn(true);
            when(mapper.selectList(any())).thenReturn(List.of(row()));

            Map<String, Object> page = service.getPage(1, 10, null, null);

            @SuppressWarnings("unchecked")
            List<HardshipFunding> rows = (List<HardshipFunding>) page.get("records");
            assertEquals(1, rows.size());
            assertNull(rows.get(0).getSensitiveNote(), "scoped role must NOT see sensitive note");
        }

        @Test
        void sensitiveNoteVisibleForUnrestrictedRole() {
            DataScopeFilter filter = new DataScopeFilter();
            filter.setUnrestricted(true);
            when(dataScopeService.currentFilter()).thenReturn(filter);
            when(mapper.selectList(any())).thenReturn(List.of(row()));

            Map<String, Object> page = service.getPage(1, 10, null, null);

            @SuppressWarnings("unchecked")
            List<HardshipFunding> rows = (List<HardshipFunding>) page.get("records");
            assertEquals("家庭变故细节", rows.get(0).getSensitiveNote());
        }

        @Test
        void rowHiddenEntirelyWithoutScope() {
            // 教师：无该生范围 → 整行不可见
            DataScopeFilter filter = new DataScopeFilter();
            when(dataScopeService.currentFilter()).thenReturn(filter);
            when(dataScopeService.canAccessStudent(any())).thenReturn(false);
            when(mapper.selectList(any())).thenReturn(List.of(row()));

            Map<String, Object> page = service.getPage(1, 10, null, null);

            @SuppressWarnings("unchecked")
            List<HardshipFunding> rows = (List<HardshipFunding>) page.get("records");
            assertTrue(rows.isEmpty());
        }

        @Test
        void duplicateYearTypeApplicationRejected() {
            DataScopeFilter filter = new DataScopeFilter();
            filter.setUnrestricted(true);
            when(dataScopeService.currentFilter()).thenReturn(filter);
            when(dataScopeService.canAccessStudent(any())).thenReturn(true); // unrestricted 也走此检查
            when(mapper.selectCount(any())).thenReturn(1L);
            HardshipFunding f = row();
            f.setAcademicYear("2025-2026");
            f.setFundingType("GRANT");
            assertThrows(BusinessException.class, () -> service.addApplication(f));
        }
    }

    // ---------- 预警 ----------

    @org.junit.jupiter.api.Nested
    class Warning {

        private StudentWarningMapper mapper;
        private StudentWarningServiceImpl service;

        @BeforeEach
        void setUp() {
            mapper = mock(StudentWarningMapper.class);
            service = new StudentWarningServiceImpl();
            ReflectionTestUtils.setField(service, "baseMapper", mapper);
            ReflectionTestUtils.setField(service, "studentMapper", mock(StudentMapper.class));
            ReflectionTestUtils.setField(service, "notificationSender", mock(NotificationSender.class));
            when(mapper.insert(any(StudentWarning.class))).thenReturn(1);
        }

        @Test
        void generatesOnlyAboveThreshold() {
            when(mapper.selectCount(any())).thenReturn(0L);
            Map<Long, Integer> values = new HashMap<>();
            values.put(1L, 3); // == threshold → 生成
            values.put(2L, 2); // < threshold → 跳过
            values.put(3L, 5); // > threshold → 生成

            int created = service.generateByRule("SCORE", "score_fail_3", 3, values);

            assertEquals(2, created);
            ArgumentCaptor<StudentWarning> cap = ArgumentCaptor.forClass(StudentWarning.class);
            verify(mapper, times(2)).insert(cap.capture());
            assertEquals(Long.valueOf(1L), cap.getAllValues().get(0).getStudentId());
            assertEquals(Integer.valueOf(3), cap.getAllValues().get(0).getCurrentValue());
        }

        @Test
        void idempotentForExistingActiveWarnings() {
            when(mapper.selectCount(any())).thenReturn(1L); // 已存在 ACTIVE
            Map<Long, Integer> values = new HashMap<>();
            values.put(1L, 4);

            int created = service.generateByRule("SCORE", "score_fail_3", 3, values);

            assertEquals(0, created);
            verify(mapper, never()).insert(any());
        }
    }
}
