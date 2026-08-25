import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页', icon: 'HomeFilled', roles: ['ADMIN', 'TEACHER', 'STUDENT'] }
      },
      {
        path: 'student',
        name: 'Student',
        component: () => import('@/views/Student.vue'),
        meta: { title: '学生管理', icon: 'User', roles: ['ADMIN', 'TEACHER'] }
      },
      {
        path: 'clazz',
        name: 'Clazz',
        component: () => import('@/views/Clazz.vue'),
        meta: { title: '班级管理', icon: 'OfficeBuilding', roles: ['ADMIN', 'TEACHER'] }
      },
      {
        path: 'course',
        name: 'Course',
        component: () => import('@/views/Course.vue'),
        meta: { title: '课程管理', icon: 'Reading', roles: ['ADMIN', 'TEACHER'] }
      },
      {
        path: 'score',
        name: 'Score',
        component: () => import('@/views/Score.vue'),
        meta: { title: '成绩管理', icon: 'DataAnalysis', roles: ['ADMIN', 'TEACHER', 'STUDENT'] }
      },
      {
        path: 'education',
        name: 'Education',
        component: () => import('@/views/Education.vue'),
        meta: { title: '教务管理', icon: 'Calendar', roles: ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER', 'COUNSELOR', 'TEACHER', 'STUDENT'] }
      },
      {
        path: 'score-audit',
        name: 'ScoreAudit',
        component: () => import('@/views/ScoreAudit.vue'),
        meta: { title: '成绩审核', icon: 'Checked', roles: ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER', 'TEACHER'] }
      },
      {
        path: 'affairs',
        name: 'Affairs',
        component: () => import('@/views/Affairs.vue'),
        meta: { title: '学生事务', icon: 'Notebook', roles: ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER', 'COUNSELOR', 'TEACHER', 'STUDENT'] }
      },
      {
        path: 'analysis',
        name: 'Analysis',
        component: () => import('@/views/Analysis.vue'),
        meta: { title: '成绩分析', icon: 'TrendCharts', roles: ['ADMIN', 'TEACHER'] }
      },
      {
        path: 'teacher',
        name: 'Teacher',
        component: () => import('@/views/Teacher.vue'),
        meta: { title: '教师管理', icon: 'Avatar', roles: ['ADMIN'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

function getRoleFromStorage() {
  try {
    const raw = localStorage.getItem('userInfo')
    if (raw) return JSON.parse(raw).role || ''
  } catch (e) {
    return ''
  }
  return ''
}

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path === '/login') {
    next(token ? '/' : undefined)
    return
  }
  if (!token) {
    next('/login')
    return
  }
  // 角色守卫：与后端 @PreAuthorize 保持一致，越权访问重定向到首页
  const roles = to.meta?.roles
  if (Array.isArray(roles) && roles.length) {
    const role = getRoleFromStorage()
    if (!roles.includes(role)) {
      next('/dashboard')
      return
    }
  }
  next()
})

export default router
