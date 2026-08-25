package com.example.student.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.common.ImportResult;
import com.example.student.entity.Teacher;
import com.example.student.excel.TeacherExcel;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TeacherService extends IService<Teacher> {

    List<Teacher> getAllTeacher();
    Teacher getTeacherById(Long id);

    void addTeacher(Teacher teacher);

    void updateTeacher(Teacher teacher);

    void deleteTeacher(Long id);

    List<TeacherExcel> exportTeachers();

    ImportResult<TeacherExcel> importTeachers(MultipartFile file);
}
