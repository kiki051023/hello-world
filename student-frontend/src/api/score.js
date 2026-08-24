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

export function exportScore(params) {
  return request({
    url: '/score/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function importScore(data) {
  return request({
    url: '/score/import',
    method: 'post',
    data
  })
}

export function exportScoreImportFail(data) {
  return request({
    url: '/score/import-fail-export',
    method: 'post',
    data,
    responseType: 'blob'
  })
}
