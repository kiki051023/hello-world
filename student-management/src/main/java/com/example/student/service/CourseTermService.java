package com.example.student.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.CourseTerm;

import java.util.Map;

public interface CourseTermService extends IService<CourseTerm> {

    Map<String, Object> getPage(long current, long size, Long courseId, Long termId);

    CourseTerm getByIdChecked(Long id);

    void addCourseTerm(CourseTerm courseTerm);

    void updateCourseTerm(CourseTerm courseTerm);

    void deleteCourseTerm(Long id);
}
