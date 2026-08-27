package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.student.entity.Permission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 权限字典表（sys_permission）专用 Mapper。
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
}