package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.Department;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.DepartmentMapper;
import com.example.student.service.DepartmentService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department>
        implements DepartmentService {

    @Override
    public IPage<Department> getDepartmentPage(Long current, Long size, String keyword, Long schoolId) {
        return baseMapper.selectDepartmentPage(new Page<>(current, size), keyword, schoolId);
    }

    @Override
    public List<Department> getEnabledDepartments(Long schoolId) {
        return baseMapper.selectEnabled(schoolId);
    }

    @Override
    public void addDepartment(Department department) {
        if (department == null || department.getSchoolId() == null
                || !StringUtils.hasText(department.getDepartmentCode())
                || !StringUtils.hasText(department.getDepartmentName())) {
            throw new BusinessException("所属学校、系部编码和名称不能为空");
        }
        Long exists = count(new LambdaQueryWrapper<Department>()
                .eq(Department::getSchoolId, department.getSchoolId())
                .eq(Department::getDepartmentCode, department.getDepartmentCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException("该学校下系部编码已存在");
        }
        department.setId(null);
        if (department.getStatus() == null) department.setStatus(1);
        save(department);
    }

    @Override
    public void updateDepartment(Department department) {
        if (department == null || department.getId() == null) throw new BusinessException("参数错误");
        Department exist = getById(department.getId());
        if (exist == null) throw new BusinessException("系部不存在");
        Long dup = count(new LambdaQueryWrapper<Department>()
                .eq(Department::getSchoolId, department.getSchoolId())
                .eq(Department::getDepartmentCode, department.getDepartmentCode())
                .ne(Department::getId, department.getId()));
        if (dup != null && dup > 0) {
            throw new BusinessException("该学校下系部编码已存在");
        }
        updateById(department);
    }

    @Override
    public void deleteDepartment(Long id) {
        if (id == null || getById(id) == null) throw new BusinessException("系部不存在");
        removeById(id);
    }
}