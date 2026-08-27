package com.example.student.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.common.ImportResult;
import com.example.student.entity.Clazz;
import com.example.student.excel.ClazzExcel;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ClazzService extends IService<Clazz> {

    IPage<Clazz> getClazzPage(Long current, Long size, String clazzName, String grade);

    void addClazz(Clazz clazz);

    void updateClazz(Clazz clazz);

    void deleteClazz(Long id);

    List<Clazz> getAllClazz();
    Clazz getClazzById(Long id);

    List<ClazzExcel> exportClazzs(String clazzName, String grade);

    /** 班级导出行数（范围+筛选，聚合计数） */
    long countForExport(String clazzName, String grade);

    ImportResult<ClazzExcel> importClazzs(MultipartFile file);
}
