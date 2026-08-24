import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 刷新令牌并发控制：同一时间只允许一个刷新请求，其余排队等待
let isRefreshing = false
let pendingQueue = []

function flushQueue(error) {
  pendingQueue.forEach(cb => cb(error))
  pendingQueue = []
}

function clearAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('userInfo')
}

request.interceptors.response.use(
  response => {
    // 文件下载（blob）走特殊通道，原样返回 axios 响应，避免被统一结构拦截
    if (response.config && response.config.responseType === 'blob') {
      return response
    }
    const res = response.data
    if (res.code === 200) {
      return res
    }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  async error => {
    const original = error.config || {}
    // 刷新接口自身失败：直接清理并跳登录，避免自循环
    if (original._isRefresh) {
      clearAuth()
      router.push('/login')
      return Promise.reject(error)
    }
    const status = error.response && error.response.status
    if (status === 401) {
      if (isRefreshing) {
        // 已有刷新在进行，排队等待新令牌后重试
        return new Promise((resolve, reject) => {
          pendingQueue.push(err => {
            if (err) reject(err)
            else resolve(request(original))
          })
        })
      }
      isRefreshing = true
      const refreshToken = localStorage.getItem('refreshToken')
      if (!refreshToken) {
        isRefreshing = false
        clearAuth()
        router.push('/login')
        return Promise.reject(error)
      }
      try {
        const res = await request({
          url: '/auth/refresh',
          method: 'post',
          data: { refreshToken },
          _isRefresh: true
        })
        const newToken = res.data.accessToken
        localStorage.setItem('token', newToken)
        flushQueue(null)
        return request(original)
      } catch (e) {
        flushQueue(e)
        clearAuth()
        router.push('/login')
        return Promise.reject(e)
      } finally {
        isRefreshing = false
      }
    }
    const msg = (error.response && error.response.data && error.response.data.msg) || error.message || '网络错误'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
