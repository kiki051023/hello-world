package com.example.student.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.Major;

import java.util.List;

public interface MajorService extends IService<Major> {

    IPage<Major> getMajorPage(Long current, Long size, String keyword, Long departmentId);

    List<Major> getEnabledMajors(Long departmentId);

    void addMajor(Major major);

    void updateMajor(Major major);

    void deleteMajor(Long id);
}