package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.common.DistributedCacheService;
import com.example.student.entity.SysMenu;
import com.example.student.mapper.SysMenuMapper;
import com.example.student.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    private static final String CACHE_KEY = "menu:visible:";
    private static final long CACHE_TTL = 300;

    @Autowired
    private DistributedCacheService cache;

    @Override
    @SuppressWarnings("unchecked")
    public List<SysMenu> listVisibleByRole(String role) {
        // 缓存 Map 投影（Redis JSON 往返丢实体类型），读端重建
        List<Map<String, Object>> rows = (List<Map<String, Object>>) cache.getOrLoad(
                CACHE_KEY + role, CACHE_TTL,
                k -> list(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getStatus, 1).orderByAsc(SysMenu::getSort))
                        .stream().filter(m -> Arrays.asList(m.getRoles().split(",")).contains(role))
                        .map(this::toCacheRow).toList());
        return rows.stream().map(this::fromCacheRow).toList();
    }

    @Override
    public IPage<SysMenu> getPage(Long current, Long size) {
        return page(new Page<>(current, size),
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort));
    }

    /** 菜单变更后清缓存（管理端 CRUD 调用） */
    @Override
    public void evictAll() {
        cache.evictByPrefix(CACHE_KEY);
    }

    private Map<String, Object> toCacheRow(SysMenu m) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("id", m.getId());
        row.put("parentId", m.getParentId());
        row.put("title", m.getTitle());
        row.put("path", m.getPath());
        row.put("icon", m.getIcon());
        row.put("roles", m.getRoles());
        row.put("sort", m.getSort());
        return row;
    }

    private SysMenu fromCacheRow(Map<String, Object> row) {
        SysMenu m = new SysMenu();
        m.setId(row.get("id") == null ? null : ((Number) row.get("id")).longValue());
        m.setParentId(row.get("parentId") == null ? 0L : ((Number) row.get("parentId")).longValue());
        m.setTitle((String) row.get("title"));
        m.setPath((String) row.get("path"));
        m.setIcon((String) row.get("icon"));
        m.setRoles((String) row.get("roles"));
        m.setSort(row.get("sort") == null ? 0 : ((Number) row.get("sort")).intValue());
        return m;
    }
}
