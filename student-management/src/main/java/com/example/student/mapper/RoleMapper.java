package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.student.entity.DataScope;
import com.example.student.entity.Permission;
import com.example.student.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    List<Permission> selectPermissionsByUserId(@Param("userId") Long userId);
    List<DataScope> selectDataScopesByUserId(@Param("userId") Long userId);
}
