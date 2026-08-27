package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.student.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-角色关联表（sys_user_role）专用 Mapper。
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
}