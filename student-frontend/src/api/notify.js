import request from '@/utils/request'

export function getNotificationPage(params) {
  return request({ url: '/notification/page', method: 'get', params })
}
export function getUnreadCount() {
  return request({ url: '/notification/unread-count', method: 'get' })
}
export function markNotificationRead(id) {
  return request({ url: `/notification/${id}/read`, method: 'put' })
}
export function markAllNotificationsRead() {
  return request({ url: '/notification/read-all', method: 'put' })
}