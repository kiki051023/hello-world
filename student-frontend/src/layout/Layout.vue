<template>
  <el-container class="layout-container">
    <el-aside :width="isMobile ? '0px' : (isCollapsed ? '64px' : '220px')" class="sidebar"
              :class="{ 'sidebar-collapsed': isCollapsed, 'sidebar-mobile': isMobile }">
      <div class="logo">
        <div class="logo-crest" aria-hidden="true">
          <svg viewBox="0 0 32 32" width="30" height="30" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="16" cy="16" r="14" stroke="#C9A227" stroke-width="1.6" fill="none"/>
            <circle cx="16" cy="16" r="11" stroke="#C9A227" stroke-width="0.6" fill="none" opacity="0.6"/>
            <path d="M16 8 L22 19 L18.5 19 L18.5 22.5 L13.5 22.5 L13.5 19 L10 19 L16 8 Z" fill="#C9A227"/>
          </svg>
        </div>
        <span v-show="!isCollapsed" class="logo-text">学生管理系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :default-openeds="openGroups"
        :collapse="isCollapsed"
        :collapse-transition="false"
        router
        class="side-menu"
      >
        <template v-for="g in visibleGroups" :key="g.title">
          <!-- 单项组直接渲染菜单项，避免多余层级 -->
          <el-menu-item v-if="g.items.length === 1 && !g.alwaysSub" :index="g.items[0].index">
            <el-icon><component :is="g.items[0].icon" /></el-icon>
            <span>{{ g.items[0].title }}</span>
          </el-menu-item>
          <el-sub-menu v-else :index="g.title">
            <template #title>
              <el-icon><component :is="g.icon" /></el-icon>
              <span>{{ g.title }}</span>
            </template>
            <el-menu-item v-for="m in g.items" :key="m.index" :index="m.index">
              <span>{{ m.title }}</span>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-aside>

    <!-- 移动端抽屉导航 -->
    <el-drawer v-model="drawerVisible" direction="ltr" size="240px" :with-header="false" class="nav-drawer">
      <div class="drawer-inner">
        <div class="logo">
          <div class="logo-crest" aria-hidden="true">
            <svg viewBox="0 0 32 32" width="30" height="30" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="16" cy="16" r="14" stroke="#C9A227" stroke-width="1.6" fill="none"/>
              <circle cx="16" cy="16" r="11" stroke="#C9A227" stroke-width="0.6" fill="none" opacity="0.6"/>
              <path d="M16 8 L22 19 L18.5 19 L18.5 22.5 L13.5 22.5 L13.5 19 L10 19 L16 8 Z" fill="#C9A227"/>
            </svg>
          </div>
          <span class="logo-text">学生管理系统</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          :default-openeds="openGroups"
          router
          class="side-menu"
          @select="drawerVisible = false"
        >
          <template v-for="g in visibleGroups" :key="g.title">
            <el-menu-item v-if="g.items.length === 1 && !g.alwaysSub" :index="g.items[0].index">
              <el-icon><component :is="g.items[0].icon" /></el-icon>
              <span>{{ g.items[0].title }}</span>
            </el-menu-item>
            <el-sub-menu v-else :index="g.title">
              <template #title>
                <el-icon><component :is="g.icon" /></el-icon>
                <span>{{ g.title }}</span>
              </template>
              <el-menu-item v-for="m in g.items" :key="m.index" :index="m.index">
                <span>{{ m.title }}</span>
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </div>
    </el-drawer>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <!-- 汉堡按钮：桌面折叠/展开，移动端拉抽屉 -->
          <button class="collapse-btn" type="button" aria-label="切换导航" @click="toggleSidebar">
            <el-icon :size="18"><Expand v-if="isCollapsed || isMobile" /><Fold v-else /></el-icon>
          </button>
          <el-breadcrumb separator="/" class="gold-breadcrumb">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- M3-3 全局搜索：Ctrl+K / 点击输入框 -->
          <el-input class="global-search" placeholder="搜索页面 (Ctrl+K)" readonly :prefix-icon="Search"
                    @click="searchVisible = true" />
          <!-- M4-1 暗色切换 -->
          <el-icon class="theme-toggle" :title="isDark ? '切到亮色' : '切到暗色'" @click="toggleDark">
            <Sunny v-if="isDark" /><Moon v-else />
          </el-icon>
          <NotificationBell />
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <span class="user-avatar">{{ avatarChar }}</span>
              <span class="user-name">{{ userStore.realName || userStore.username }}</span>
              <el-icon class="user-caret"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <!-- M4-3 多标签页：访问过的页面缓存保活，标签可关/可刷新 -->
        <div class="tabs-bar no-print">
          <div v-for="t in tabs" :key="t.path"
               :class="['tab', { active: t.path === route.path }]"
               @click="router.push(t.path)">
            <span>{{ t.title }}</span>
            <el-icon v-if="tabs.length > 1" class="tab-close" @click.stop="closeTab(t)"><Close /></el-icon>
          </div>
        </div>
        <router-view v-slot="{ Component }">
          <!-- ponytail: 不带 include 全量缓存（页面 ~20 个，内存可控）；
               script-setup 组件 name 与路由 name 不一致时 include 会静默失效 -->
          <keep-alive>
            <component :is="Component" :key="route.path" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>

    <!-- 个人信息 → 修改密码抽屉 -->
    <ChangePasswordDrawer ref="changePwdDrawerRef" />

    <!-- M3-3 全局搜索命令面板 -->
    <el-dialog v-model="searchVisible" :show-close="false" width="440px" class="search-dialog"
               @opened="searchInputRef?.focus()">
      <el-input ref="searchInputRef" v-model="searchText" placeholder="输入页面名…" :prefix-icon="Search" />
      <div class="search-results">
        <div v-for="m in searchResults" :key="m.index" class="search-item" @click="goSearch(m)">
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ m.title }}</span>
        </div>
        <el-empty v-if="searchText && !searchResults.length" description="无匹配页面" :image-size="50" />
      </div>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import NotificationBell from '@/components/NotificationBell.vue'
import { ElMessageBox } from 'element-plus'
import {
  HomeFilled,
  User,
  OfficeBuilding,
  Reading,
  DataAnalysis,
  Avatar,
  TrendCharts,
  Calendar,
  Checked,
  Notebook,
  Suitcase,
  Monitor,
  ArrowDown,
  Expand,
  Fold,
  Collection,
  House,
  Search,
  Sunny,
  Moon,
  Close
} from '@element-plus/icons-vue'
import { logout } from '@/api/auth'
import ChangePasswordDrawer from '@/components/ChangePasswordDrawer.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const changePwdDrawerRef = ref(null)

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')

// 头像字：取真实姓名首字，无则用户名单字
const avatarChar = computed(() => {
  const name = userStore.realName || userStore.username || ''
  return name ? name.charAt(0).toUpperCase() : 'U'
})

// 菜单与角色映射：与后端 @PreAuthorize 保持一致
const allMenus = [
  { index: '/dashboard', title: '首页', icon: HomeFilled, roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { index: '/student', title: '学生管理', icon: User, roles: ['ADMIN', 'TEACHER'] },
  { index: '/clazz', title: '班级管理', icon: OfficeBuilding, roles: ['ADMIN', 'TEACHER'] },
  { index: '/course', title: '课程管理', icon: Reading, roles: ['ADMIN', 'TEACHER'] },
  { index: '/score', title: '成绩管理', icon: DataAnalysis, roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { index: '/system/terms', title: '学期管理', icon: Calendar, roles: ['ADMIN', 'EDUCATION_ADMIN'] },
  { index: '/education', title: '教务管理', icon: Calendar, roles: ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER', 'COUNSELOR', 'TEACHER', 'STUDENT'] },
  { index: '/score-audit', title: '成绩审核', icon: Checked, roles: ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER', 'TEACHER'] },
  { index: '/affairs', title: '学生事务', icon: Notebook, roles: ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER', 'COUNSELOR', 'TEACHER', 'STUDENT'] },
  { index: '/practice', title: '实习就业', icon: Suitcase, roles: ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER', 'COUNSELOR', 'TEACHER', 'PRACTICE_TEACHER', 'STUDENT'] },
  { index: '/ops', title: '运维监控', icon: Monitor, roles: ['ADMIN'] },
  { index: '/system/org', title: '组织架构', icon: OfficeBuilding, roles: ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER', 'COUNSELOR'] },
  { index: '/system/users', title: '用户管理', icon: User, roles: ['ADMIN'] },
  { index: '/system/roles', title: '角色管理', icon: Avatar, roles: ['ADMIN'] },
  { index: '/system/dict', title: '字典管理', icon: Collection, roles: ['ADMIN'] },
  { index: '/system/dorm', title: '宿舍管理', icon: House, roles: ['ADMIN'] },
  { index: '/portal', title: '我的门户', icon: User, roles: ['STUDENT'] },
  { index: '/timetable', title: '我的课表', icon: Calendar, roles: ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER', 'COUNSELOR', 'TEACHER', 'STUDENT'] },
  { index: '/analysis', title: '成绩分析', icon: TrendCharts, roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { index: '/teacher', title: '教师管理', icon: Avatar, roles: ['ADMIN'] }
]

const byIndex = Object.fromEntries(allMenus.map((m) => [m.index, m]))

// 导航分组：单页组直接平铺，多页组折叠
const menuGroups = [
  {
    title: '总览',
    icon: HomeFilled,
    items: ['/dashboard', '/portal', '/analysis'].map((i) => byIndex[i])
  },
  {
    title: '教务中心',
    icon: Calendar,
    items: ['/score', '/system/terms', '/score-audit', '/education', '/timetable'].map((i) => byIndex[i])
  },
  {
    title: '学务中心',
    icon: User,
    items: ['/student', '/clazz', '/course', '/teacher', '/affairs', '/practice'].map((i) => byIndex[i])
  },
  {
    title: '系统',
    icon: Monitor,
    items: ['/ops', '/system/org', '/system/users', '/system/roles', '/system/dict', '/system/dorm'].map((i) => byIndex[i]),
    alwaysSub: true
  }
]

const visibleGroups = computed(() =>
  menuGroups
    .map((g) => ({ ...g, items: g.items.filter((m) => m && Array.isArray(m.roles) && m.roles.includes(userStore.role)) }))
    .filter((g) => g.items.length > 0)
)

// 当前路由所在组自动展开
const openGroups = computed(() =>
  visibleGroups.value
    .filter((g) => g.alwaysSub !== true || g.items.length > 0)
    .filter((g) => g.items.some((m) => m.index === route.path))
    .map((g) => g.title)
)

// ============ M3-3 全局搜索（Ctrl+K） ============
const searchVisible = ref(false)
const searchText = ref('')
const searchInputRef = ref(null)

const searchResults = computed(() => {
  const q = searchText.value.trim()
  const mine = allMenus.filter((m) => m.roles.includes(userStore.role))
  if (!q) return mine
  return mine.filter((m) => m.title.includes(q))
})

function goSearch(m) {
  searchVisible.value = false
  searchText.value = ''
  router.push(m.index)
}

function onGlobalKey(e) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    searchVisible.value = true
  }
}

// ============ M4-1 暗色主题 ============
const isDark = ref(localStorage.getItem('theme-dark') === '1')
function applyDark() {
  document.documentElement.classList.toggle('dark', isDark.value)
}
function toggleDark() {
  isDark.value = !isDark.value
  localStorage.setItem('theme-dark', isDark.value ? '1' : '0')
  applyDark()
}

// ============ M4-3 多标签页 ============
const tabs = ref([{ path: '/dashboard', title: '首页' }])
watch(() => route.path, (p) => {
  if (p === '/login') return
  if (!tabs.value.some((t) => t.path === p)) {
    tabs.value.push({ path: p, title: route.meta.title || p })
  }
})
function closeTab(t) {
  const i = tabs.value.findIndex((x) => x.path === t.path)
  tabs.value.splice(i, 1)
  if (route.path === t.path) {
    router.push(tabs.value[Math.max(0, i - 1)].path)
  }
}

onMounted(() => {
  userStore.loadUserInfo()
  applyDark()
  // 移动端断点监听：<768px 隐藏侧栏（改用抽屉）
  checkViewport()
  window.addEventListener('resize', checkViewport)
  window.addEventListener('keydown', onGlobalKey)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkViewport)
  window.removeEventListener('keydown', onGlobalKey)
})

// ============ 侧栏折叠 + 移动端抽屉（UE-1） ============
const STORAGE_KEY = 'sidebar-collapsed'
const isCollapsed = ref(localStorage.getItem(STORAGE_KEY) === '1')
const isMobile = ref(false)
const drawerVisible = ref(false)

function checkViewport() {
  isMobile.value = window.innerWidth < 768
  if (isMobile.value) drawerVisible.value = false
}

function toggleSidebar() {
  if (isMobile.value) {
    drawerVisible.value = true
  } else {
    isCollapsed.value = !isCollapsed.value
    localStorage.setItem(STORAGE_KEY, isCollapsed.value ? '1' : '0')
  }
}

function handleCommand(command) {
  if (command === 'profile') {
    changePwdDrawerRef.value?.open()
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      try {
        await logout()
      } finally {
        userStore.logout()
        router.push('/login')
      }
    }).catch(() => {})
  }
}
</script>

<style scoped>
/* ============ 响应式布局（UE-1）：侧栏折叠 + 抽屉 + 筛选区/表格 ============ */
.layout-container {
  height: 100vh;
}

/* 折叠按钮 */
.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 6px;
  color: #5a6B7e;
  cursor: pointer;
  margin-right: 12px;
  transition: background 0.2s;
  flex-shrink: 0;
}
.collapse-btn:hover {
  background: var(--parchment-200);
  color: #c9a227;
}

/* 移动端隐藏侧栏，抽屉接管 */
.sidebar-mobile {
  display: none;
}

/* 折叠过渡 */

/* ============ 侧栏：深墨藏蓝 + 金色 ============ */
.sidebar {
  background: linear-gradient(180deg, #0e1116 0%, #141a24 100%);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(201, 162, 39, 0.22);
  flex-shrink: 0;
}

.logo-text {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: #f0ead9;
}

/* 菜单：去 Element 深蓝默认，改深墨 + 金 */
.side-menu {
  border-right: none;
  background: transparent;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 0;
}

.side-menu :deep(.el-menu-item),
.side-menu :deep(.el-sub-menu__title) {
  height: 46px;
  line-height: 46px;
  color: #a6adb4;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.side-menu :deep(.el-menu-item:hover),
.side-menu :deep(.el-sub-menu__title:hover) {
  color: #f0ead9;
  background: rgba(201, 162, 39, 0.08);
}

/* 激活项：金色文字 + 左侧金色竖条 */
.side-menu :deep(.el-menu-item.is-active) {
  color: #d4af37;
  background: rgba(201, 162, 39, 0.12);
  position: relative;
}

.side-menu :deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: 0 2px 2px 0;
  background: linear-gradient(180deg, #d4af37, #9c7d22);
}

/* 子菜单展开级背景略深，与深墨分层 */
.side-menu :deep(.el-sub-menu .el-menu) {
  background: rgba(0, 0, 0, 0.22);
}

/* 当前展开组的目录标题：淡金 */
.side-menu :deep(.el-sub-menu.is-opened > .el-sub-menu__title) {
  color: #c9a227;
}

/* 图标统一柔和金，激活时亮金 */
.side-menu :deep(.el-icon) {
  color: #8a8571;
  transition: color 0.25s;
}

.side-menu :deep(.el-menu-item.is-active .el-icon) {
  color: #d4af37;
}

/* ============ 顶栏：羊皮纸 + 金 ============ */
.header {
  background-color: #faf9f6;
  border-bottom: 1px solid var(--line-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 60px;
}

.gold-breadcrumb :deep(.el-breadcrumb__inner),
.gold-breadcrumb :deep(.el-breadcrumb__inner a) {
  color: var(--ink-muted);
  font-weight: 400;
}

.gold-breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--brass-600);
  font-weight: 600;
}

.gold-breadcrumb :deep(.el-breadcrumb__separator) {
  color: #c9a227;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  color: #3f4a5a;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-info:hover {
  background: var(--parchment-200);
}

.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: #14171d;
  background: linear-gradient(180deg, #d4af37, #b8942a);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.35);
}

.user-name {
  font-size: 14px;
  font-weight: 500;
}

.user-caret {
  color: #9a937f;
  font-size: 12px;
}

/* ============ 内容区：暖白羊皮纸 ============ */
.main-content {
  background-color: var(--parchment-100);
  padding: 20px;
  overflow-y: auto;
}

/* 窄屏：隐藏头像下方用户名，保留箭头 */
@media (max-width: 768px) {
  .user-name {
    display: none;
  }
  .logo-text {
    font-size: 15px;
  }
}

/* M3-3 全局搜索 */
.global-search { width: 180px; margin-right: 12px; cursor: pointer; }
.global-search :deep(.el-input__wrapper) { cursor: pointer; }
@media (max-width: 768px) { .global-search { display: none; } }
/* M4-1 暗色切换 */
.theme-toggle { margin-right: 12px; cursor: pointer; font-size: 17px; color: var(--ink-muted); }
.theme-toggle:hover { color: var(--brass-500); }

/* M4-3 多标签页 */
.tabs-bar { display: flex; gap: 4px; flex-wrap: wrap; margin-bottom: 10px; }
.tab {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 3px 12px; font-size: 12px; border-radius: 4px;
  border: 1px solid var(--el-border-color); cursor: pointer;
  color: var(--el-text-color-secondary); background: var(--el-fill-color-blank);
}
.tab.active { color: var(--brass-600); border-color: var(--line-gold); }
.tab-close { font-size: 12px; }
.tab-close:hover { color: var(--el-color-error); }
.search-results { margin-top: 10px; max-height: 320px; overflow-y: auto; }
.search-item {
  display: flex; align-items: center; gap: 10px; padding: 9px 12px;
  border-radius: 6px; cursor: pointer; font-size: 14px;
}
.search-item:hover { background: var(--parchment-100, #f5f5f4); }
</style>