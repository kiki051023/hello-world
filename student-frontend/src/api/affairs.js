import request from '@/utils/request'

// ===== 学籍异动 =====
export function changeStatus(studentId, params) {
  return request({ url: `/student-status/${studentId}/change`, method: 'post', params })
}
export function statusHistoryPage(params) {
  return request({ url: '/student-status/history/page', method: 'get', params })
}
export function studentStatusHistory(studentId) {
  return request({ url: `/student-status/${studentId}/history`, method: 'get' })
}

// ===== 请假 =====
export function getLeavePage(params) {
  return request({ url: '/leave/page', method: 'get', params })
}
export function submitLeave(data) {
  return request({ url: '/leave', method: 'post', data })
}
export function approveLeave(id, pass, comment) {
  return request({ url: `/leave/${id}/approve`, method: 'post', params: { pass, comment } })
}
export function returnLeave(id) {
  return request({ url: `/leave/${id}/return`, method: 'post' })
}

// ===== 奖惩 =====
export function getRpPage(params) {
  return request({ url: '/reward-punishment/page', method: 'get', params })
}
export function addRewardPunishment(data) {
  return request({ url: '/reward-punishment', method: 'post', data })
}
export function revokePunishment(id, note) {
  return request({ url: `/reward-punishment/${id}/revoke`, method: 'post', params: { note } })
}

// ===== 资助 =====
export function getFundingPage(params) {
  return request({ url: '/funding/page', method: 'get', params })
}
export function addFunding(data) {
  return request({ url: '/funding', method: 'post', data })
}
export function reviewFunding(id, pass, comment) {
  return request({ url: `/funding/${id}/review`, method: 'post', params: { pass, comment } })
}

// ===== 预警 =====
export function getWarningPage(params) {
  return request({ url: '/warning/page', method: 'get', params })
}
export function generateScoreFailWarning(threshold) {
  return request({ url: '/warning/generate/score-fail', method: 'post', params: { threshold } })
}
export function handleWarning(id, dismiss, note) {
  return request({ url: `/warning/${id}/handle`, method: 'post', params: { dismiss, note } })
}
