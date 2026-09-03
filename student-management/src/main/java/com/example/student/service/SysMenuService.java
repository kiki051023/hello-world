package com.example.student.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.SysMenu;

import java.util.List;

/** 动态菜单（M4-6） */
public interface SysMenuService extends IService<SysMenu> {

    /** 当前角色可见的启用菜单（sort 升序，走缓存） */
    List<SysMenu> listVisibleByRole(String role);

    IPage<SysMenu> getPage(Long current, Long size);

    /** 菜单变更后清全部角色缓存 */
    void evictAll();
}
