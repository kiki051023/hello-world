import request from '@/utils/request'

// ===== 实习分配 =====
export function getAssignmentPage(params) {
  return request({ url: '/practice-assignment/page', method: 'get', params })
}
export function assignPractice(data) {
  return request({ url: '/practice-assignment', method: 'post', data })
}
export function finishPractice(id, score) {
  return request({ url: `/practice-assignment/${id}/finish`, method: 'post', params: { score } })
}

// ===== 实习周记 =====
export function getLogPage(params) {
  return request({ url: '/practice-log/page', method: 'get', params })
}
export function submitLog(data) {
  return request({ url: '/practice-log', method: 'post', data })
}
export function commentLog(id, comment) {
  return request({ url: `/practice-log/${id}/comment`, method: 'post', params: { comment } })
}

// ===== 就业与证书 =====
export function getEmploymentPage(params) {
  return request({ url: '/employment/page', method: 'get', params })
}
export function addEmployment(data) {
  return request({ url: '/employment', method: 'post', data })
}
export function confirmEmployment(id) {
  return request({ url: `/employment/${id}/confirm`, method: 'post' })
}
export function employmentStats(params) {
  return request({ url: '/employment/stats', method: 'get', params })
}
export function getCertPage(params) {
  return request({ url: '/employment/certificate/page', method: 'get', params })
}
export function addCertificate(data) {
  return request({ url: '/employment/certificate', method: 'post', data })
}
