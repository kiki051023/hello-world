package com.example.student.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.DictData;
import com.example.student.entity.DictType;

import java.util.List;

/** 数据字典（M2-1） */
public interface DictService extends IService<DictType> {

    /** 类型分页（可按名称/类型模糊） */
    IPage<DictType> getTypePage(Long current, Long size, String keyword);

    /** 某类型下启用数据（sort 升序），前端下拉/翻译用 */
    List<DictData> listEnabledData(String dictType);

    /** 数据分页 */
    IPage<DictData> getDataPage(Long current, Long size, String dictType);

    /** 新增类型（重复 dictType 拒绝） */
    void addType(DictType type);

    /** 修改类型（dictType 不可改，避免数据失联） */
    void updateType(DictType type);

    /** 删除类型（下有数据拒绝） */
    void deleteType(Long id);

    /** 新增数据（同类型下 value 重复拒绝） */
    void addData(DictData data);

    /** 修改数据 */
    void updateData(DictData data);

    /** 删除数据 */
    void deleteData(Long id);

    /** 翻译：dictType+value → label，无匹配原样返回 value */
    String translate(String dictType, String value);
}
