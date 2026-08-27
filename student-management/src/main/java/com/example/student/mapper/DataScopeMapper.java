package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.student.entity.DataScope;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据权限范围表（sys_data_scope）专用 Mapper。
 * 独立 Mapper 而非挂在 RoleMapper 下，确保 BaseMapper 的 CRUD 解析到正确表名。
 */
@Mapper
public interface DataScopeMapper extends BaseMapper<DataScope> {
}