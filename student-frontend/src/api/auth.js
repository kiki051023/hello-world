import request from '@/utils/request'

export function getCaptcha() {
  return request({
    url: '/auth/captcha',
    method: 'get'
  })
}

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

export function refresh(refreshToken) {
  return request({
    url: '/auth/refresh',
    method: 'post',
    data: { refreshToken },
    _isRefresh: true
  })
}

export function changePassword(data) {
  return request({
    url: '/auth/change-password',
    method: 'post',
    data
  })
}

export function forceChangePassword(data) {
  return request({
    url: '/auth/force-change-password',
    method: 'post',
    data
  })
}