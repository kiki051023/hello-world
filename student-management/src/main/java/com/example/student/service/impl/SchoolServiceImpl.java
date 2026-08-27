package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.School;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.SchoolMapper;
import com.example.student.service.SchoolService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SchoolServiceImpl extends ServiceImpl<SchoolMapper, School> implements SchoolService {

    @Override
    public IPage<School> getSchoolPage(Long current, Long size, String keyword) {
        return baseMapper.selectSchoolPage(new Page<>(current, size), keyword);
    }

    @Override
    public List<School> getEnabledSchools() {
        return baseMapper.selectEnabled();
    }

    @Override
    public void addSchool(School school) {
        if (school == null || !StringUtils.hasText(school.getSchoolCode())
                || !StringUtils.hasText(school.getSchoolName())) {
            throw new BusinessException("学校编码和名称不能为空");
        }
        Long exists = count(new LambdaQueryWrapper<School>()
                .eq(School::getSchoolCode, school.getSchoolCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException("学校编码已存在");
        }
        school.setId(null);
        if (school.getStatus() == null) school.setStatus(1);
        save(school);
    }

    @Override
    public void updateSchool(School school) {
        if (school == null || school.getId() == null) throw new BusinessException("参数错误");
        School exist = getById(school.getId());
        if (exist == null) throw new BusinessException("学校不存在");
        Long dup = count(new LambdaQueryWrapper<School>()
                .eq(School::getSchoolCode, school.getSchoolCode())
                .ne(School::getId, school.getId()));
        if (dup != null && dup > 0) {
            throw new BusinessException("学校编码已存在");
        }
        updateById(school);
    }

    @Override
    public void deleteSchool(Long id) {
        if (id == null || getById(id) == null) throw new BusinessException("学校不存在");
        removeById(id);
    }
}