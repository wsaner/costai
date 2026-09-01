import request from '@/utils/request'

export function listProject(query) {
  return request({ url: '/cost/project/list', method: 'get', params: query })
}

export function getProject(id) {
  return request({ url: `/cost/project/${id}`, method: 'get' })
}

export function addProject(data) {
  return request({ url: '/cost/project', method: 'post', data })
}

export function updateProject(data) {
  return request({ url: '/cost/project', method: 'put', data })
}

export function deleteProject(ids) {
  return request({ url: `/cost/project/${ids}`, method: 'delete' })
}

export function changeProjectStatus(data) {
  return request({ url: '/cost/project/changeStatus', method: 'put', data })
}

export function getProjectStatistics(query) {
  return request({ url: '/cost/project/statistics', method: 'get', params: query })
}

export function listProjectManagers(keyword) {
  return request({ url: '/cost/project/managerOptions', method: 'get', params: { keyword } })
}
