import request from '@/utils/request'

const base = '/dorm'
export const page = (params) => request({ url: base + '/page', method: 'get', params })
export const add = (data) => request({ url: base, method: 'post', data })
export const update = (data) => request({ url: base, method: 'put', data })
export const remove = (id) => request({ url: base + '/' + id, method: 'delete' })
