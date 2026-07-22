package com.example.student.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.Clazz;

import java.util.List;

public interface ClazzService extends IService<Clazz> {

    IPage<Clazz> getClazzPage(Long current, Long size, String clazzName, String grade);

    void addClazz(Clazz clazz);

    void updateClazz(Clazz clazz);

    void deleteClazz(Long id);

    List<Clazz> getAllClazz();
}
