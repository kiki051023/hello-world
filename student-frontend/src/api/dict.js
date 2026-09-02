import request from '@/utils/request'

// ---------- 公开查询 ----------
export function listDictData(dictType) {
  return request({ url: `/system/dict/data/list/${dictType}`, method: 'get' })
}

// ---------- 类型管理 ----------
export function getDictTypePage(params) {
  return request({ url: '/system/dict/type/page', method: 'get', params })
}
export function addDictType(data) {
  return request({ url: '/system/dict/type', method: 'post', data })
}
export function updateDictType(data) {
  return request({ url: '/system/dict/type', method: 'put', data })
}
export function deleteDictType(id) {
  return request({ url: `/system/dict/type/${id}`, method: 'delete' })
}

// ---------- 数据管理 ----------
export function getDictDataPage(params) {
  return request({ url: '/system/dict/data/page', method: 'get', params })
}
export function addDictData(data) {
  return request({ url: '/system/dict/data', method: 'post', data })
}
export function updateDictData(data) {
  return request({ url: '/system/dict/data', method: 'put', data })
}
export function deleteDictData(id) {
  return request({ url: `/system/dict/data/${id}`, method: 'delete' })
}
