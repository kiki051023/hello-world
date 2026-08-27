import request from '@/utils/request'

// 用户管理（仅管理员）
export function getUserPage(params) {
  return request({ url: '/system/users/page', method: 'get', params })
}
export function getUserDetail(id) {
  return request({ url: `/system/users/${id}`, method: 'get' })
}
export function createUser(data) {
  return request({ url: '/system/users', method: 'post', data })
}
export function updateUser(id, data) {
  return request({ url: `/system/users/${id}`, method: 'put', data })
}
export function resetUserPassword(id, newPassword) {
  return request({ url: `/system/users/${id}/reset-password`, method: 'post', params: { newPassword } })
}
export function changeUserStatus(id, status) {
  return request({ url: `/system/users/${id}/status`, method: 'put', params: { status } })
}
export function assignUserRoles(id, roleIds) {
  return request({ url: `/system/users/${id}/roles`, method: 'put', data: roleIds })
}
export function assignUserDataScopes(id, scopes) {
  return request({ url: `/system/users/${id}/data-scope`, method: 'put', data: scopes })
}
export function getUserRoles(id) {
  return request({ url: `/system/users/${id}/roles`, method: 'get' })
}