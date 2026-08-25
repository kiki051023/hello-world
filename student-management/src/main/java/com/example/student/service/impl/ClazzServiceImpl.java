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
import com.example.student.security.DataScopeFilter;
import com.example.student.security.DataScopeService;
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

    @Autowired
    private DataScopeService dataScopeService;

    @Override
    public IPage<Clazz> getClazzPage(Long current, Long size, String clazzName, String grade) {
        Page<Clazz> page = new Page<>(current, size);
        return baseMapper.selectClazzPage(page, clazzName, grade, dataScopeService.currentFilter());
    }

    @Override
    public void addClazz(Clazz clazz) {
        DataScopeFilter scope = dataScopeService.currentFilter();
        if (!scope.isUnrestricted() && scope.getDepartmentId() == null) throw new org.springframework.security.access.AccessDeniedException("无权新增班级");
        if (!scope.isUnrestricted() && clazz.getDepartmentId() == null) clazz.setDepartmentId(scope.getDepartmentId());
        baseMapper.insert(clazz);
    }

    @Override
    public void updateClazz(Clazz clazz) {
        Clazz existing = baseMapper.selectById(clazz.getId());
        if (existing == null || !dataScopeService.canAccessClazz(existing)) throw new org.springframework.security.access.AccessDeniedException("无权修改该班级");
        baseMapper.updateById(clazz);
    }

    @Override
    public void deleteClazz(Long id) {
        Clazz existing = baseMapper.selectById(id);
        if (existing == null || !dataScopeService.canAccessClazz(existing)) throw new org.springframework.security.access.AccessDeniedException("无权删除该班级");
        baseMapper.deleteById(id);
    }

    @Override
    public List<Clazz> getAllClazz() {
        // SQL 级数据范围过滤，替代 selectList(null)+内存过滤
        return baseMapper.selectScoped(currentScope());
    }

    @Override
    public Clazz getClazzById(Long id) {
        Clazz clazz = baseMapper.selectById(id);
        if (clazz != null && !dataScopeService.canAccessClazz(clazz)) throw new org.springframework.security.access.AccessDeniedException("无权查看该班级");
        return clazz;
    }

    @Override
    public List<ClazzExcel> exportClazzs(String clazzName, String grade) {
        LambdaQueryWrapper<Clazz> wrapper = new LambdaQueryWrapper<>();
        if (clazzName != null && !clazzName.isEmpty()) wrapper.like(Clazz::getClazzName, clazzName);
        if (grade != null && !grade.isEmpty()) wrapper.eq(Clazz::getGrade, grade);
        List<Clazz> list = baseMapper.selectList(wrapper).stream().filter(dataScopeService::canAccessClazz).toList();
        // 教师 id→name 轻量投影（仅两列），替代教师全表实体加载
        Map<Long, String> teacherMap = new java.util.HashMap<>();
        for (Map<String, Object> row : teacherMapper.selectNamesByIds(
                list.stream().map(Clazz::getHeadTeacherId).filter(java.util.Objects::nonNull).distinct().toList())) {
            teacherMap.put(((Number) row.get("id")).longValue(), (String) row.get("name"));
        }
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
            DataScopeFilter scope = dataScopeService.currentFilter();
            if (!scope.isUnrestricted() && (scope.isSelfOnly() || scope.getDepartmentId() == null))
                throw new org.springframework.security.access.AccessDeniedException("当前角色没有班级导入范围");
            List<?> rows = EasyExcel.read(file.getInputStream()).head(ClazzExcel.class).sheet().doReadSync();
            List<ClazzExcel> data = rows.stream().map(o -> (ClazzExcel) o).collect(Collectors.toList());
            ImportLimits.requireAllowedRowCount(data.size());
            result.setTotal(data.size());
            List<String> clazzNames = data.stream().map(ClazzExcel::getClazzName)
                    .filter(name -> name != null && !name.isBlank()).distinct().toList();
            Set<String> existing = clazzNames.isEmpty() ? new java.util.HashSet<>() :
                    baseMapper.selectList(new LambdaQueryWrapper<Clazz>()
                                    .select(Clazz::getClazzName).in(Clazz::getClazzName, clazzNames))
                            .stream().map(Clazz::getClazzName).collect(Collectors.toSet());
            List<String> teacherNames = data.stream().map(ClazzExcel::getHeadTeacherName)
                    .filter(name -> name != null && !name.isBlank()).distinct().toList();
            Map<String, Long> teacherNameMap = teacherNames.isEmpty() ? new java.util.HashMap<>() :
                    teacherMapper.selectList(new LambdaQueryWrapper<Teacher>()
                                    .select(Teacher::getId, Teacher::getName).in(Teacher::getName, teacherNames))
                            .stream().collect(Collectors.toMap(Teacher::getName, Teacher::getId, (a, b) -> a));
            Set<String> importedNames = new java.util.HashSet<>();
            List<Clazz> entities = new ArrayList<>();
            for (ClazzExcel e : data) {
                if (e.getClazzName() == null || e.getClazzName().isEmpty()) {
                    e.setErrorMessage("班级名称不能为空");
                    result.getFailRows().add(e);
                    continue;
                }
                if (!importedNames.add(e.getClazzName()) || existing.contains(e.getClazzName())) {
                    e.setErrorMessage("班级已存在(" + e.getClazzName() + ")");
                    result.getFailRows().add(e);
                    continue;
                }
                Clazz c = new Clazz();
                c.setClazzName(e.getClazzName());
                c.setGrade(e.getGrade());
                if (!scope.isUnrestricted()) c.setDepartmentId(scope.getDepartmentId());
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

    private DataScopeFilter currentScope() {
        return dataScopeService == null ? null : dataScopeService.currentFilter();
    }
}
