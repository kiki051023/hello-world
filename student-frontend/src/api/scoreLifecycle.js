import request from '@/utils/request'

// 成绩生命周期：提交/审核/退回/作废/补考/重修/历史/学分
export function submitScore(id) {
  return request({ url: `/score-lifecycle/${id}/submit`, method: 'post' })
}
export function approveScore(id) {
  return request({ url: `/score-lifecycle/${id}/approve`, method: 'post' })
}
export function rejectScore(id, reason) {
  return request({ url: `/score-lifecycle/${id}/reject`, method: 'post', params: { reason } })
}
export function voidScore(id, reason) {
  return request({ url: `/score-lifecycle/${id}/void`, method: 'post', params: { reason } })
}
export function makeupScore(id, newScore, courseTermId) {
  return request({ url: `/score-lifecycle/${id}/makeup`, method: 'post', params: { newScore, courseTermId } })
}
export function retakeScore(id, newScore, newCourseTermId) {
  return request({ url: `/score-lifecycle/${id}/retake`, method: 'post', params: { newScore, newCourseTermId } })
}
export function scoreHistory(id) {
  return request({ url: `/score-lifecycle/${id}/history`, method: 'get' })
}
export function creditSummary(params) {
  return request({ url: '/score-lifecycle/credit', method: 'get', params })
}
