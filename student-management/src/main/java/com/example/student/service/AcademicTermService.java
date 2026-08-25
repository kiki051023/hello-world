package com.example.student.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.AcademicTerm;

public interface AcademicTermService extends IService<AcademicTerm> {

    void addTerm(AcademicTerm term);

    void updateTerm(AcademicTerm term);
}
