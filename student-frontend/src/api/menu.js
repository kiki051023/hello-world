import request from '@/utils/request'

// M4-6 动态菜单
export function getMyMenus() {
  return request({ url: '/system/menu/my', method: 'get' })
}
export function getMenuPage(params) {
  return request({ url: '/system/menu/page', method: 'get', params })
}
export function addMenu(data) {
  return request({ url: '/system/menu', method: 'post', data })
}
export function updateMenu(data) {
  return request({ url: '/system/menu', method: 'put', data })
}
export function deleteMenu(id) {
  return request({ url: `/system/menu/${id}`, method: 'delete' })
}
