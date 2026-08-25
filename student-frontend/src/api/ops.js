import request from '@/utils/request'

// 运维：审计日志查询
export function getAuditLogs(params) {
  return request({ url: '/system/operation-logs', method: 'get', params })
}
export function getImportTasks(params) {
  return request({ url: '/system/import-tasks', method: 'get', params })
}
