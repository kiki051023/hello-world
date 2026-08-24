import request from '@/utils/request'

export function getClazzPage(params) {
  return request({
    url: '/clazz/page',
    method: 'get',
    params
  })
}

export function getClazzList() {
  return request({
    url: '/clazz/list',
    method: 'get'
  })
}

export function getClazz(id) {
  return request({
    url: `/clazz/${id}`,
    method: 'get'
  })
}

export function addClazz(data) {
  return request({
    url: '/clazz',
    method: 'post',
    data
  })
}

export function updateClazz(data) {
  return request({
    url: '/clazz',
    method: 'put',
    data
  })
}

export function deleteClazz(id) {
  return request({
    url: `/clazz/${id}`,
    method: 'delete'
  })
}

export function exportClazz(params) {
  return request({
    url: '/clazz/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function importClazz(data) {
  return request({
    url: '/clazz/import',
    method: 'post',
    data
  })
}

export function exportClazzImportFail(data) {
  return request({
    url: '/clazz/import-fail-export',
    method: 'post',
    data,
    responseType: 'blob'
  })
}
