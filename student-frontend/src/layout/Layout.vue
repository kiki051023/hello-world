<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="sidebar">
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

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/" class="gold-breadcrumb">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
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
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
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
  ArrowDown
} from '@element-plus/icons-vue'
import { logout } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

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
  { index: '/education', title: '教务管理', icon: Calendar, roles: ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER', 'COUNSELOR', 'TEACHER', 'STUDENT'] },
  { index: '/score-audit', title: '成绩审核', icon: Checked, roles: ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER', 'TEACHER'] },
  { index: '/affairs', title: '学生事务', icon: Notebook, roles: ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER', 'COUNSELOR', 'TEACHER', 'STUDENT'] },
  { index: '/practice', title: '实习就业', icon: Suitcase, roles: ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER', 'COUNSELOR', 'TEACHER', 'PRACTICE_TEACHER', 'STUDENT'] },
  { index: '/ops', title: '运维监控', icon: Monitor, roles: ['ADMIN'] },
  { index: '/analysis', title: '成绩分析', icon: TrendCharts, roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { index: '/teacher', title: '教师管理', icon: Avatar, roles: ['ADMIN'] }
]

const byIndex = Object.fromEntries(allMenus.map((m) => [m.index, m]))

// 导航分组：单页组直接平铺，多页组折叠
const menuGroups = [
  {
    title: '总览',
    icon: HomeFilled,
    items: ['/dashboard', '/analysis'].map((i) => byIndex[i])
  },
  {
    title: '教务中心',
    icon: Calendar,
    items: ['/score', '/score-audit', '/education'].map((i) => byIndex[i])
  },
  {
    title: '学务中心',
    icon: User,
    items: ['/student', '/clazz', '/course', '/teacher', '/affairs', '/practice'].map((i) => byIndex[i])
  },
  {
    title: '系统',
    icon: Monitor,
    items: ['/ops'].map((i) => byIndex[i]),
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

onMounted(() => {
  userStore.loadUserInfo()
})

function handleCommand(command) {
  if (command === 'logout') {
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
.layout-container {
  height: 100vh;
}

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
</style>