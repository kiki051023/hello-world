package com.example.student.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.student.entity.DictData;
import com.example.student.mapper.DictDataMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** M2-1 字典翻译：命中返回 label，未命中原样返回，null/空安全 */
class DictServiceTranslateTest {

    @Mock
    private DictDataMapper dictDataMapper;

    @InjectMocks
    private com.example.student.service.impl.DictServiceImpl dictService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(dictService, "baseMapper", null);
    }

    @Test
    void translateHitReturnsLabel() {
        DictData d = new DictData();
        d.setDictLabel("男");
        when(dictDataMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(d);
        assertEquals("男", dictService.translate("gender", "M"));
    }

    @Test
    void translateMissReturnsOriginalValue() {
        when(dictDataMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertEquals("X", dictService.translate("gender", "X"));
    }

    @Test
    void translateNullAndEmptySafe() {
        assertEquals(null, dictService.translate("gender", null));
        assertEquals("", dictService.translate("gender", ""));
    }
}
