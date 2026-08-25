package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.AcademicTerm;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.AcademicTermMapper;
import com.example.student.service.AcademicTermService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicTermServiceImpl extends ServiceImpl<AcademicTermMapper, AcademicTerm>
        implements AcademicTermService {

    @Override
    @Transactional
    public void addTerm(AcademicTerm term) {
        validate(term);
        Long exists = baseMapper.selectCount(new LambdaQueryWrapper<AcademicTerm>()
                .eq(AcademicTerm::getAcademicYear, term.getAcademicYear())
                .eq(AcademicTerm::getTermNo, term.getTermNo()));
        if (exists != null && exists > 0) throw new BusinessException("该学年学期已存在");
        if (Integer.valueOf(1).equals(term.getStatus())) baseMapper.resetCurrentFlag();
        baseMapper.insert(term);
    }

    @Override
    @Transactional
    public void updateTerm(AcademicTerm term) {
        if (baseMapper.selectById(term.getId()) == null) throw new BusinessException("学期不存在");
        if (Integer.valueOf(1).equals(term.getStatus())) baseMapper.resetCurrentFlag();
        baseMapper.updateById(term);
    }

    private void validate(AcademicTerm term) {
        if (term.getAcademicYear() == null || term.getAcademicYear().isBlank())
            throw new BusinessException("学年不能为空");
        if (term.getTermNo() == null || term.getTermNo() < 1 || term.getTermNo() > 3)
            throw new BusinessException("学期序号须为 1-3");
    }
}
