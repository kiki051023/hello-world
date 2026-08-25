package com.example.student.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.common.ImportLimits;
import com.example.student.common.ImportResult;
import com.example.student.entity.Clazz;
import com.example.student.entity.Teacher;
import com.example.student.exception.BusinessException;
import com.example.student.excel.ClazzExcel;
import com.example.student.mapper.ClazzMapper;
import com.example.student.mapper.TeacherMapper;
import com.example.student.service.ClazzService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ClazzServiceImpl extends ServiceImpl<ClazzMapper, Clazz> implements ClazzService {

    @Autowired
    private TeacherMapper teacherMapper;

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

    @Override
    public List<ClazzExcel> exportClazzs(String clazzName, String grade) {
        LambdaQueryWrapper<Clazz> wrapper = new LambdaQueryWrapper<>();
        if (clazzName != null && !clazzName.isEmpty()) wrapper.like(Clazz::getClazzName, clazzName);
        if (grade != null && !grade.isEmpty()) wrapper.eq(Clazz::getGrade, grade);
        List<Clazz> list = baseMapper.selectList(wrapper);
        Map<Long, String> teacherMap = teacherMapper.selectList(null).stream()
                .collect(Collectors.toMap(Teacher::getId, Teacher::getName, (a, b) -> a));
        List<ClazzExcel> result = new ArrayList<>();
        for (Clazz c : list) {
            ClazzExcel e = new ClazzExcel();
            e.setClazzName(c.getClazzName());
            e.setGrade(c.getGrade());
            e.setHeadTeacherName(c.getHeadTeacherId() != null ? teacherMap.get(c.getHeadTeacherId()) : null);
            result.add(e);
        }
        return result;
    }

    @Override
    @Transactional
    public ImportResult<ClazzExcel> importClazzs(MultipartFile file) {
        ImportResult<ClazzExcel> result = new ImportResult<>();
        try {
            List<?> rows = EasyExcel.read(file.getInputStream()).head(ClazzExcel.class).sheet().doReadSync();
            List<ClazzExcel> data = rows.stream().map(o -> (ClazzExcel) o).collect(Collectors.toList());
            ImportLimits.requireAllowedRowCount(data.size());
            result.setTotal(data.size());
            Set<String> existing = baseMapper.selectList(null).stream()
                    .map(Clazz::getClazzName).collect(Collectors.toSet());
            Map<String, Long> teacherNameMap = teacherMapper.selectList(null).stream()
                    .collect(Collectors.toMap(Teacher::getName, Teacher::getId, (a, b) -> a));
            List<Clazz> entities = new ArrayList<>();
            for (ClazzExcel e : data) {
                if (e.getClazzName() == null || e.getClazzName().isEmpty()) {
                    e.setErrorMessage("班级名称不能为空");
                    result.getFailRows().add(e);
                    continue;
                }
                if (existing.contains(e.getClazzName())) {
                    e.setErrorMessage("班级已存在(" + e.getClazzName() + ")");
                    result.getFailRows().add(e);
                    continue;
                }
                Clazz c = new Clazz();
                c.setClazzName(e.getClazzName());
                c.setGrade(e.getGrade());
                if (e.getHeadTeacherName() != null && !e.getHeadTeacherName().isEmpty()) {
                    Long tid = teacherNameMap.get(e.getHeadTeacherName());
                    if (tid == null) {
                        e.setErrorMessage("班主任不存在(" + e.getHeadTeacherName() + ")");
                        result.getFailRows().add(e);
                        continue;
                    }
                    c.setHeadTeacherId(tid);
                }
                entities.add(c);
            }
            if (!entities.isEmpty()) {
                saveBatch(entities);
                result.setSuccessCount(entities.size());
            }
        } catch (IOException ex) {
            throw new BusinessException("Excel 解析失败: " + ex.getMessage());
        }
        return result;
    }
}
