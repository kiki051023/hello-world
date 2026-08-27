package com.example.student.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.School;

import java.util.List;

public interface SchoolService extends IService<School> {

    IPage<School> getSchoolPage(Long current, Long size, String keyword);

    List<School> getEnabledSchools();

    void addSchool(School school);

    void updateSchool(School school);

    void deleteSchool(Long id);
}