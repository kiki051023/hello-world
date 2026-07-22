package com.example.student.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.Student;

public interface StudentService extends IService<Student> {

    IPage<Student> getStudentPage(Long current, Long size, String name, String studentNo, Long clazzId);

    void addStudent(Student student);

    void updateStudent(Student student);

    void deleteStudent(Long id);

    Student getStudentById(Long id);
}
