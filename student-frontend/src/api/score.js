import request from '@/utils/request'

export function getScorePage(params) {
  return request({
    url: '/score/page',
    method: 'get',
    params
  })
}

export function getScore(id) {
  return request({
    url: `/score/${id}`,
    method: 'get'
  })
}

export function addScore(data) {
  return request({
    url: '/score',
    method: 'post',
    data
  })
}

export function updateScore(data) {
  return request({
    url: '/score',
    method: 'put',
    data
  })
}

export function deleteScore(id) {
  return request({
    url: `/score/${id}`,
    method: 'delete'
  })
}

export function getScoreStats(params) {
  return request({
    url: '/score/stats',
    method: 'get',
    params
  })
}
