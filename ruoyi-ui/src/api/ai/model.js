import request from '@/utils/request'

export function listModelConfig(query) {
  return request({ url: '/ai/model-configs/list', method: 'get', params: query })
}

export function getModelConfig(id) {
  return request({ url: `/ai/model-configs/${id}`, method: 'get' })
}

export function addModelConfig(data) {
  return request({ url: '/ai/model-configs', method: 'post', data })
}

export function updateModelConfig(data) {
  return request({ url: '/ai/model-configs', method: 'put', data })
}

export function deleteModelConfig(ids) {
  return request({ url: `/ai/model-configs/${ids}`, method: 'delete' })
}

export function testModelConfig(id) {
  return request({ url: `/ai/model-configs/${id}/test`, method: 'post' })
}
