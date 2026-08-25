package com.example.student.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.common.ImportLimits;
import com.example.student.common.ImportResult;
import com.example.student.entity.Clazz;
import com.example.student.entity.Student;
import com.example.student.exception.BusinessException;
import com.example.student.excel.StudentExcel;
import com.example.student.mapper.ClazzMapper;
import com.example.student.mapper.StudentMapper;
import com.example.student.service.StudentService;
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
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    @Autowired
    private ClazzMapper clazzMapper;

    @Override
    public IPage<Student> getStudentPage(Long current, Long size, String name, String studentNo, Long clazzId) {
        Page<Student> page = new Page<>(current, size);
        return baseMapper.selectStudentPage(page, name, studentNo, clazzId);
    }

    @Override
    public void addStudent(Student student) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getStudentNo, student.getStudentNo());
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("学号已存在");
        }
        baseMapper.insert(student);
    }

    @Override
    public void updateStudent(Student student) {
        if (student.getId() == null) {
            throw new BusinessException("学生ID不能为空");
        }
        baseMapper.updateById(student);
    }

    @Override
    public void deleteStudent(Long id) {
        baseMapper.deleteById(id);
    }

    @Override
    public Student getStudentById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<StudentExcel> exportStudents(String name, String studentNo, Long clazzId) {
        List<Map<String, Object>> rows = baseMapper.selectStudentExport(name, studentNo, clazzId);
        List<StudentExcel> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            StudentExcel e = new StudentExcel();
            e.setStudentNo((String) row.get("studentNo"));
            e.setName((String) row.get("name"));
            e.setGender((String) row.get("gender"));
            Object birthday = row.get("birthday");
            if (birthday instanceof java.time.LocalDate localDate) e.setBirthday(localDate);
            if (birthday instanceof java.sql.Date sqlDate) e.setBirthday(sqlDate.toLocalDate());
            e.setPhone((String) row.get("phone"));
            e.setClazzName((String) row.get("clazzName"));
            e.setAddress((String) row.get("address"));
            result.add(e);
        }
        return result;
    }

    @Override
    @Transactional
    public ImportResult<StudentExcel> importStudents(MultipartFile file) {
        ImportResult<StudentExcel> result = new ImportResult<>();
        try {
            List<?> rows = EasyExcel.read(file.getInputStream()).head(StudentExcel.class).sheet().doReadSync();
            List<StudentExcel> data = rows.stream().map(o -> (StudentExcel) o).collect(Collectors.toList());
            ImportLimits.requireAllowedRowCount(data.size());
            result.setTotal(data.size());
            List<String> studentNos = data.stream().map(StudentExcel::getStudentNo)
                    .filter(no -> no != null && !no.isBlank()).distinct().toList();
            Set<String> existingNos = studentNos.isEmpty() ? new java.util.HashSet<>() :
                    baseMapper.selectIdsByStudentNos(studentNos).stream()
                            .map(row -> (String) row.get("studentNo")).collect(Collectors.toSet());
            Set<String> importedNos = new java.util.HashSet<>();
            Map<String, Long> clazzNameMap = clazzMapper.selectList(null).stream()
                    .collect(Collectors.toMap(Clazz::getClazzName, Clazz::getId, (a, b) -> a));
            List<Student> entities = new ArrayList<>();
            for (StudentExcel e : data) {
                StringBuilder err = new StringBuilder();
                if (e.getStudentNo() == null || e.getStudentNo().isEmpty()) err.append("学号不能为空;");
                if (e.getName() == null || e.getName().isEmpty()) err.append("姓名不能为空;");
                if (err.length() > 0) {
                    e.setErrorMessage(err.toString());
                    result.getFailRows().add(e);
                    continue;
                }
                if (!importedNos.add(e.getStudentNo()) || existingNos.contains(e.getStudentNo())) {
                    e.setErrorMessage("学号已存在(" + e.getStudentNo() + ")");
                    result.getFailRows().add(e);
                    continue;
                }
                Student s = new Student();
                s.setStudentNo(e.getStudentNo());
                s.setName(e.getName());
                s.setGender(e.getGender());
                s.setBirthday(e.getBirthday());
                s.setPhone(e.getPhone());
                if (e.getClazzName() != null && !e.getClazzName().isEmpty()) {
                    Long cid = clazzNameMap.get(e.getClazzName());
                    if (cid == null) {
                        e.setErrorMessage("班级不存在(" + e.getClazzName() + ")");
                        result.getFailRows().add(e);
                        continue;
                    }
                    s.setClazzId(cid);
                }
                s.setAddress(e.getAddress());
                entities.add(s);
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
