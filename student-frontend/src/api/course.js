import request from '@/utils/request'

export function getCoursePage(params) {
  return request({
    url: '/course/page',
    method: 'get',
    params
  })
}

export function getCourseList() {
  return request({
    url: '/course/list',
    method: 'get'
  })
}

export function getCourse(id) {
  return request({
    url: `/course/${id}`,
    method: 'get'
  })
}

export function addCourse(data) {
  return request({
    url: '/course',
    method: 'post',
    data
  })
}

export function updateCourse(data) {
  return request({
    url: '/course',
    method: 'put',
    data
  })
}

export function deleteCourse(id) {
  return request({
    url: `/course/${id}`,
    method: 'delete'
  })
}
