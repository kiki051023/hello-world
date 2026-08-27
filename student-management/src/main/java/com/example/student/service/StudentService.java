package com.example.student.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.common.ImportResult;
import com.example.student.entity.Student;
import com.example.student.excel.StudentExcel;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StudentService extends IService<Student> {

    IPage<Student> getStudentPage(Long current, Long size, String name, String studentNo, Long clazzId);

    void addStudent(Student student);

    void updateStudent(Student student);

    void deleteStudent(Long id);

    Student getStudentById(Long id);

    List<StudentExcel> exportStudents(String name, String studentNo, Long clazzId);

    /** 学生导出行数（范围+筛选，聚合计数） */
    long countForExport(String name, String studentNo, Long clazzId);

    ImportResult<StudentExcel> importStudents(MultipartFile file);
}
