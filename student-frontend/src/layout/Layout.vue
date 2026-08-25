<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <el-icon size="28" color="#409EFF"><School /></el-icon>
        <span>学生管理系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#001529"
        text-color="#a6adb4"
        active-text-color="#ffffff"
      >
        <el-menu-item v-for="m in visibleMenus" :key="m.index" :index="m.index">
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ m.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><UserFilled /></el-icon>
              {{ userStore.realName || userStore.username }}
              <el-icon><ArrowDown /></el-icon>
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
  Suitcase
} from '@element-plus/icons-vue'
import { logout } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')

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
  { index: '/analysis', title: '成绩分析', icon: TrendCharts, roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { index: '/teacher', title: '教师管理', icon: Avatar, roles: ['ADMIN'] }
]

const visibleMenus = computed(() =>
  allMenus.filter((m) => m.roles.includes(userStore.role))
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

.sidebar {
  background-color: #001529;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #1f3a5f;
}

.el-menu {
  border-right: none;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #606266;
}

.main-content {
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}
</style>
