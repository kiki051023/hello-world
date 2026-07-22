package com.example.student.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.Teacher;

import java.util.List;

public interface TeacherService extends IService<Teacher> {

    List<Teacher> getAllTeacher();

    void addTeacher(Teacher teacher);

    void updateTeacher(Teacher teacher);

    void deleteTeacher(Long id);
}
