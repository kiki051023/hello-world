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
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
    List<DataScope> selectDataScopesByUserId(@Param("userId") Long userId);

    /** 查询某用户拥有的全部角色（分页/分配界面用） */
    List<Role> selectRolesByUserId(@Param("userId") Long userId);

    /** 查询某角色已关联的全部权限 */
    List<Permission> selectPermissionsByRoleId(@Param("roleId") Long roleId);

    /** 查询某角色下的数据范围模板 */
    List<DataScope> selectDataScopesByRoleId(@Param("roleId") Long roleId);
}
