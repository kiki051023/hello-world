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
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      {
        path: 'student',
        name: 'Student',
        component: () => import('@/views/Student.vue'),
        meta: { title: '学生管理', icon: 'User' }
      },
      {
        path: 'clazz',
        name: 'Clazz',
        component: () => import('@/views/Clazz.vue'),
        meta: { title: '班级管理', icon: 'OfficeBuilding' }
      },
      {
        path: 'course',
        name: 'Course',
        component: () => import('@/views/Course.vue'),
        meta: { title: '课程管理', icon: 'Reading' }
      },
      {
        path: 'score',
        name: 'Score',
        component: () => import('@/views/Score.vue'),
        meta: { title: '成绩管理', icon: 'DataAnalysis' }
      },
      {
        path: 'teacher',
        name: 'Teacher',
        component: () => import('@/views/Teacher.vue'),
        meta: { title: '教师管理', icon: 'Avatar' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path === '/login') {
    if (token) {
      next('/')
    } else {
      next()
    }
  } else {
    if (token) {
      next()
    } else {
      next('/login')
    }
  }
})

export default router
