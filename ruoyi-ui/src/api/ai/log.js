import request from '@/utils/request'

export function listAiRequestLog(query) {
  return request({ url: '/ai/request-logs/list', method: 'get', params: query })
}
export function getAiRequestLog(id) {
  return request({ url: `/ai/request-logs/${id}`, method: 'get' })
}
