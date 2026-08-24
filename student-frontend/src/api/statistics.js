import request from '@/utils/request'

// 数据大屏概览
export function getOverview() {
  return request({
    url: '/statistics/overview',
    method: 'get'
  })
}

// 成绩深度分析（分布/班级/课程/Top/趋势/及格率）
export function getScoreAnalysis() {
  return request({
    url: '/statistics/score-analysis',
    method: 'get'
  })
}

// 单个学生各课程成绩雷达
export function getStudentRadar(studentId) {
  return request({
    url: `/statistics/student-radar/${studentId}`,
    method: 'get'
  })
}
