package com.example.student.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.common.ImportLimits;
import com.example.student.common.ImportResult;
import com.example.student.entity.Teacher;
import com.example.student.exception.BusinessException;
import com.example.student.excel.TeacherExcel;
import com.example.student.mapper.TeacherMapper;
import com.example.student.security.DataScopeFilter;
import com.example.student.security.DataScopeService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private DataScopeService dataScopeService;

    private DataScopeFilter currentScope() {
        return dataScopeService == null ? null : dataScopeService.currentFilter();
    }

    @Override
    public List<Teacher> getAllTeacher() {
        // SQL 级数据范围过滤，替代 selectList(null)+内存过滤
        return baseMapper.selectScoped(currentScope());
    }

    @Override
    public Teacher getTeacherById(Long id) {
        Teacher teacher = baseMapper.selectById(id);
        if (teacher != null && !dataScopeService.canAccessTeacher(teacher)) throw new org.springframework.security.access.AccessDeniedException("无权查看该教师");
        return teacher;
    }

    @Override
    public void addTeacher(Teacher teacher) {
        var scope = dataScopeService.currentFilter();
        if (!scope.isUnrestricted() && scope.getDepartmentId() == null) throw new org.springframework.security.access.AccessDeniedException("无权新增教师");
        if (!scope.isUnrestricted() && teacher.getDepartmentId() == null) teacher.setDepartmentId(scope.getDepartmentId());
        baseMapper.insert(teacher);
    }

    @Override
    public void updateTeacher(Teacher teacher) {
        Teacher existing = baseMapper.selectById(teacher.getId());
        if (existing == null || !dataScopeService.canAccessTeacher(existing)) throw new org.springframework.security.access.AccessDeniedException("无权修改该教师");
        baseMapper.updateById(teacher);
    }

    @Override
    public void deleteTeacher(Long id) {
        Teacher existing = baseMapper.selectById(id);
        if (existing == null || !dataScopeService.canAccessTeacher(existing)) throw new org.springframework.security.access.AccessDeniedException("无权删除该教师");
        baseMapper.deleteById(id);
    }

    @Override
        public long countForExport() {
            return baseMapper.countScoped(dataScopeService.currentFilter());
        }

        @Override
        public List<TeacherExcel> exportTeachers() {
        List<Teacher> list = getAllTeacher();
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
            if (!dataScopeService.currentFilter().isUnrestricted())
                throw new org.springframework.security.access.AccessDeniedException("当前角色没有教师导入范围");
            List<?> rows = EasyExcel.read(file.getInputStream()).head(TeacherExcel.class).sheet().doReadSync();
            List<TeacherExcel> data = rows.stream().map(o -> (TeacherExcel) o).collect(Collectors.toList());
            ImportLimits.requireAllowedRowCount(data.size());
            result.setTotal(data.size());
            List<String> teacherNos = data.stream().map(TeacherExcel::getTeacherNo)
                    .filter(no -> no != null && !no.isBlank()).distinct().toList();
            Set<String> existing = teacherNos.isEmpty() ? new java.util.HashSet<>() :
                    baseMapper.selectList(new LambdaQueryWrapper<Teacher>()
                                    .select(Teacher::getTeacherNo).in(Teacher::getTeacherNo, teacherNos))
                            .stream().map(Teacher::getTeacherNo).collect(Collectors.toSet());
            Set<String> importedNos = new java.util.HashSet<>();
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
                if (!importedNos.add(e.getTeacherNo()) || existing.contains(e.getTeacherNo())) {
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
