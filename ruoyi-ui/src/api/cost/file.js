import request from '@/utils/request'

export function listProjectFiles(projectId) {
  return request({ url: `/cost/project/${projectId}/files/list`, method: 'get' })
}

export function getProjectFile(id) {
  return request({ url: `/cost/project/files/${id}`, method: 'get' })
}

export function updateProjectFileCategory(id, fileCategory) {
  return request({ url: `/cost/project/files/${id}/category`, method: 'put', data: { fileCategory } })
}

export function deleteProjectFile(id) {
  return request({ url: `/cost/project/files/${id}`, method: 'delete' })
}

export function getProjectFileParseStatus(id) {
  return request({ url: `/cost/project/files/${id}/parse-status`, method: 'get' })
}
