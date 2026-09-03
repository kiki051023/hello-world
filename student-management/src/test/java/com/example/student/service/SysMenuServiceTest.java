package com.example.student.service;

import com.example.student.common.DistributedCacheService;
import com.example.student.entity.SysMenu;
import com.example.student.mapper.SysMenuMapper;
import com.example.student.service.impl.SysMenuServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** M4-6 菜单：按角色过滤 + 缓存读穿 */
class SysMenuServiceTest {

    private SysMenuMapper mapper;
    private SysMenuServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(SysMenuMapper.class);
        service = new SysMenuServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "cache", new DistributedCacheService(null));
    }

    private SysMenu menu(String title, String roles) {
        SysMenu m = new SysMenu();
        m.setTitle(title);
        m.setPath("/" + title);
        m.setRoles(roles);
        m.setSort(1);
        return m;
    }

    @Test
    void listVisibleByRoleFiltersByRolesColumn() {
        when(mapper.selectList(any())).thenReturn(List.of(
                menu("dashboard", "ADMIN,TEACHER,STUDENT"),
                menu("usermanage", "ADMIN"),
                menu("portal", "STUDENT")));

        List<SysMenu> admin = service.listVisibleByRole("ADMIN");
        assertEquals(2, admin.size());

        // 缓存路径二次读（同一实例本地缓存，断言过滤结果一致）
        List<SysMenu> student = service.listVisibleByRole("STUDENT");
        assertEquals(2, student.size());
        assertEquals("dashboard", student.get(0).getTitle());
        assertEquals("portal", student.get(1).getTitle());
    }
}
