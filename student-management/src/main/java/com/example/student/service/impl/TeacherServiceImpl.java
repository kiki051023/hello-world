package com.example.student.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.common.ImportLimits;
import com.example.student.common.ImportResult;
import com.example.student.entity.Teacher;
import com.example.student.exception.BusinessException;
import com.example.student.excel.TeacherExcel;
import com.example.student.mapper.TeacherMapper;
import com.example.student.service.TeacherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, Teacher> implements TeacherService {

    @Override
    public List<Teacher> getAllTeacher() {
        return baseMapper.selectList(null);
    }

    @Override
    public void addTeacher(Teacher teacher) {
        baseMapper.insert(teacher);
    }

    @Override
    public void updateTeacher(Teacher teacher) {
        baseMapper.updateById(teacher);
    }

    @Override
    public void deleteTeacher(Long id) {
        baseMapper.deleteById(id);
    }

    @Override
    public List<TeacherExcel> exportTeachers() {
        List<Teacher> list = baseMapper.selectList(null);
        List<TeacherExcel> result = new ArrayList<>();
        for (Teacher t : list) {
            TeacherExcel e = new TeacherExcel();
            e.setTeacherNo(t.getTeacherNo());
            e.setName(t.getName());
            e.setGender(t.getGender());
            e.setPhone(t.getPhone());
            result.add(e);
        }
        return result;
    }

    @Override
    @Transactional
    public ImportResult<TeacherExcel> importTeachers(MultipartFile file) {
        ImportResult<TeacherExcel> result = new ImportResult<>();
        try {
            List<?> rows = EasyExcel.read(file.getInputStream()).head(TeacherExcel.class).sheet().doReadSync();
            List<TeacherExcel> data = rows.stream().map(o -> (TeacherExcel) o).collect(Collectors.toList());
            ImportLimits.requireAllowedRowCount(data.size());
            result.setTotal(data.size());
            Set<String> existing = baseMapper.selectList(null).stream()
                    .map(Teacher::getTeacherNo).collect(Collectors.toSet());
            List<Teacher> entities = new ArrayList<>();
            for (TeacherExcel e : data) {
                StringBuilder err = new StringBuilder();
                if (e.getTeacherNo() == null || e.getTeacherNo().isEmpty()) err.append("教师编号不能为空;");
                if (e.getName() == null || e.getName().isEmpty()) err.append("姓名不能为空;");
                if (err.length() > 0) {
                    e.setErrorMessage(err.toString());
                    result.getFailRows().add(e);
                    continue;
                }
                if (existing.contains(e.getTeacherNo())) {
                    e.setErrorMessage("教师编号已存在(" + e.getTeacherNo() + ")");
                    result.getFailRows().add(e);
                    continue;
                }
                Teacher t = new Teacher();
                t.setTeacherNo(e.getTeacherNo());
                t.setName(e.getName());
                t.setGender(e.getGender());
                t.setPhone(e.getPhone());
                entities.add(t);
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
