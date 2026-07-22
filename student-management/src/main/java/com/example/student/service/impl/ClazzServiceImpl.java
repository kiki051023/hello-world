package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.Clazz;
import com.example.student.mapper.ClazzMapper;
import com.example.student.service.ClazzService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClazzServiceImpl extends ServiceImpl<ClazzMapper, Clazz> implements ClazzService {

    @Override
    public IPage<Clazz> getClazzPage(Long current, Long size, String clazzName, String grade) {
        Page<Clazz> page = new Page<>(current, size);
        return baseMapper.selectClazzPage(page, clazzName, grade);
    }

    @Override
    public void addClazz(Clazz clazz) {
        baseMapper.insert(clazz);
    }

    @Override
    public void updateClazz(Clazz clazz) {
        baseMapper.updateById(clazz);
    }

    @Override
    public void deleteClazz(Long id) {
        baseMapper.deleteById(id);
    }

    @Override
    public List<Clazz> getAllClazz() {
        return baseMapper.selectList(null);
    }
}
