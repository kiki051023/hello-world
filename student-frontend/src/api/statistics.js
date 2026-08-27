import request from '@/utils/request'

// 数据大屏概览
export function getOverview() {
  return request({
    url: '/statistics/overview',
    method: 'get'
  })
}

// 成绩深度分析（分布/班级/课程/Top/趋势/及格率），支持 clazzId/courseId/termId 筛选
export function getScoreAnalysis(params) {
  return request({
    url: '/statistics/score-analysis',
    method: 'get',
    params
  })
}

// 单个学生各课程成绩雷达
export function getStudentRadar(studentId) {
  return request({
    url: `/statistics/student-radar/${studentId}`,
    method: 'get'
  })
}

// 学期列表（全局筛选器用）
export function getTermList() {
  return request({
    url: '/academic-term/list',
    method: 'get'
  })
}
