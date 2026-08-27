package com.example.student.service;

import com.example.student.service.impl.ScoreServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 学期名归一化模糊匹配单元测试（Excel 导入学期列） */
class TermNormalizeTest {

    @Test
    void normalizesCommonTermNameVariantsToSameKey() {
        String base = ScoreServiceImpl.normalizeTerm("2025-2026学年第一学期");
        assertEquals(base, ScoreServiceImpl.normalizeTerm("2025-2026-1"));
        assertEquals(base, ScoreServiceImpl.normalizeTerm("2025—2026第1学期"));
        assertEquals(base, ScoreServiceImpl.normalizeTerm("2025－2026学年第1学期"));
        assertEquals(base, ScoreServiceImpl.normalizeTerm(" 2025-2026 第一学期 "));
    }

    @Test
    void chineseNumeralTermConvertsToDigit() {
        assertEquals(ScoreServiceImpl.normalizeTerm("2024-2025-2"),
                ScoreServiceImpl.normalizeTerm("2024-2025学年第二学期"));
    }

    @Test
    void differentTermsStayDifferent() {
        String t1 = ScoreServiceImpl.normalizeTerm("2025-2026-1");
        String t2 = ScoreServiceImpl.normalizeTerm("2025-2026-2");
        assertEquals(false, t1.equals(t2));
    }
}
