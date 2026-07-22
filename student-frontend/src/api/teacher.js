import request from '@/utils/request'

export function getTeacherList() {
  return request({
    url: '/teacher/list',
    method: 'get'
  })
}

export function getTeacher(id) {
  return request({
    url: `/teacher/${id}`,
    method: 'get'
  })
}

export function addTeacher(data) {
  return request({
    url: '/teacher',
    method: 'post',
    data
  })
}

export function updateTeacher(data) {
  return request({
    url: '/teacher',
    method: 'put',
    data
  })
}

export function deleteTeacher(id) {
  return request({
    url: `/teacher/${id}`,
    method: 'delete'
  })
}
