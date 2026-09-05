import request from '@/utils/request'

export function listPrompt(query) {
  return request({ url: '/ai/prompts/list', method: 'get', params: query })
}
export function getPrompt(id) {
  return request({ url: `/ai/prompts/${id}`, method: 'get' })
}
export function addPrompt(data) {
  return request({ url: '/ai/prompts', method: 'post', data })
}
export function updatePrompt(data) {
  return request({ url: '/ai/prompts', method: 'put', data })
}
export function deletePrompt(ids) {
  return request({ url: `/ai/prompts/${ids}`, method: 'delete' })
}
