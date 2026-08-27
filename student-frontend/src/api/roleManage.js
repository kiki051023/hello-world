import request from '@/utils/request'

// 角色管理（仅管理员）
export function getRolePage(params) {
  return request({ url: '/system/roles/page', method: 'get', params })
}
export function getRoleList() {
  return request({ url: '/system/roles/list', method: 'get' })
}
export function getPermissionDict() {
  return request({ url: '/system/roles/permissions', method: 'get' })
}
export function getRoleDetail(id) {
  return request({ url: `/system/roles/${id}`, method: 'get' })
}
export function createRole(data) {
  return request({ url: '/system/roles', method: 'post', data })
}
export function updateRole(id, data) {
  return request({ url: `/system/roles/${id}`, method: 'put', data })
}
export function deleteRole(id) {
  return request({ url: `/system/roles/${id}`, method: 'delete' })
}
export function assignRolePermissions(id, permissionIds) {
  return request({ url: `/system/roles/${id}/permissions`, method: 'put', data: permissionIds })
}
export function assignRoleDataScopes(id, scopes) {
  return request({ url: `/system/roles/${id}/data-scope`, method: 'put', data: scopes })
}