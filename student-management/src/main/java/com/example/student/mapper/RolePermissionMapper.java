package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.student.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色-权限关联表（sys_role_permission）专用 Mapper。
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
}