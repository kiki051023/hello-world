import request from '@/utils/request'

export function getStudentPage(params) {
  return request({
    url: '/student/page',
    method: 'get',
    params
  })
}

export function getStudent(id) {
  return request({
    url: `/student/${id}`,
    method: 'get'
  })
}

export function addStudent(data) {
  return request({
    url: '/student',
    method: 'post',
    data
  })
}

export function updateStudent(data) {
  return request({
    url: '/student',
    method: 'put',
    data
  })
}

export function deleteStudent(id) {
  return request({
    url: `/student/${id}`,
    method: 'delete'
  })
}
