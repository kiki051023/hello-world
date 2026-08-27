package com.example.student.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.Department;

import java.util.List;

public interface DepartmentService extends IService<Department> {

    IPage<Department> getDepartmentPage(Long current, Long size, String keyword, Long schoolId);

    List<Department> getEnabledDepartments(Long schoolId);

    void addDepartment(Department department);

    void updateDepartment(Department department);

    void deleteDepartment(Long id);
}