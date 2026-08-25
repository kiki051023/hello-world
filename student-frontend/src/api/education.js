import request from '@/utils/request'

// ===== 学年学期 =====
export function listTerms(params) {
  return request({ url: '/academic-term/list', method: 'get', params })
}
export function addTerm(data) {
  return request({ url: '/academic-term', method: 'post', data })
}

// ===== 课程开设 =====
export function getCourseTermPage(params) {
  return request({ url: '/course-term/page', method: 'get', params })
}
export function addCourseTerm(data) {
  return request({ url: '/course-term', method: 'post', data })
}
export function updateCourseTerm(data) {
  return request({ url: '/course-term', method: 'put', data })
}
export function deleteCourseTerm(id) {
  return request({ url: `/course-term/${id}`, method: 'delete' })
}

// ===== 授课任务 =====
export function getTeachingTaskPage(params) {
  return request({ url: '/teaching-task/page', method: 'get', params })
}
export function addTeachingTask(data) {
  return request({ url: '/teaching-task', method: 'post', data })
}
export function deleteTeachingTask(id) {
  return request({ url: `/teaching-task/${id}`, method: 'delete' })
}

// ===== 培养方案 =====
export function getTrainingPlanPage(params) {
  return request({ url: '/training-plan/page', method: 'get', params })
}
export function addTrainingPlan(data) {
  return request({ url: '/training-plan', method: 'post', data })
}

// ===== 选课 =====
export function getSelectionPage(params) {
  return request({ url: '/course-selection/page', method: 'get', params })
}
export function selectCourse(data) {
  return request({ url: '/course-selection', method: 'post', data })
}
export function cancelSelection(id) {
  return request({ url: `/course-selection/${id}`, method: 'delete' })
}
