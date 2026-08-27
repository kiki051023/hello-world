import request from '@/utils/request'

// ========== 学校 ==========
export function getSchoolPage(params) {
  return request({ url: '/org/school/page', method: 'get', params })
}
export function getSchoolList() {
  return request({ url: '/org/school/list', method: 'get' })
}
export function addSchool(data) {
  return request({ url: '/org/school', method: 'post', data })
}
export function updateSchool(data) {
  return request({ url: '/org/school', method: 'put', data })
}
export function deleteSchool(id) {
  return request({ url: `/org/school/${id}`, method: 'delete' })
}

// ========== 系部 ==========
export function getDepartmentPage(params) {
  return request({ url: '/org/department/page', method: 'get', params })
}
export function getDepartmentList(params) {
  return request({ url: '/org/department/list', method: 'get', params })
}
export function addDepartment(data) {
  return request({ url: '/org/department', method: 'post', data })
}
export function updateDepartment(data) {
  return request({ url: '/org/department', method: 'put', data })
}
export function deleteDepartment(id) {
  return request({ url: `/org/department/${id}`, method: 'delete' })
}

// ========== 专业 ==========
export function getMajorPage(params) {
  return request({ url: '/org/major/page', method: 'get', params })
}
export function getMajorList(params) {
  return request({ url: '/org/major/list', method: 'get', params })
}
export function addMajor(data) {
  return request({ url: '/org/major', method: 'post', data })
}
export function updateMajor(data) {
  return request({ url: '/org/major', method: 'put', data })
}
export function deleteMajor(id) {
  return request({ url: `/org/major/${id}`, method: 'delete' })
}

// ========== 三级级联树 ==========
export function getOrgTree() {
  return request({ url: '/org/tree', method: 'get' })
}