import request from '@/utils/request'

export function listConversations(query) {
  return request({ url: '/ai/chat/conversations/list', method: 'get', params: query })
}
export function getConversation(id) {
  return request({ url: `/ai/chat/conversations/${id}`, method: 'get' })
}
export function listMessages(id, query) {
  return request({ url: `/ai/chat/conversations/${id}/messages`, method: 'get', params: query })
}
export function listChatProjectOptions(query) {
  return request({ url: '/ai/chat/conversations/project-options', method: 'get', params: query })
}
export function createConversation(data) {
  return request({ url: '/ai/chat/conversations', method: 'post', data })
}
export function updateConversation(data) {
  return request({ url: '/ai/chat/conversations', method: 'put', data })
}
export function deleteConversation(id) {
  return request({ url: `/ai/chat/conversations/${id}`, method: 'delete' })
}
