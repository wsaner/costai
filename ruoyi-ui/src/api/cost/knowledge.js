import request from '@/utils/request'

export function listKnowledgeBases(query) { return request({ url: '/cost/knowledge/list', method: 'get', params: query }) }
export function getKnowledgeBase(id) { return request({ url: `/cost/knowledge/${id}`, method: 'get' }) }
export function addKnowledgeBase(data) { return request({ url: '/cost/knowledge', method: 'post', data }) }
export function updateKnowledgeBase(data) { return request({ url: '/cost/knowledge', method: 'put', data }) }
export function deleteKnowledgeBase(id) { return request({ url: `/cost/knowledge/${id}`, method: 'delete' }) }
export function listKnowledgeDocuments(baseId) { return request({ url: `/cost/knowledge/${baseId}/documents`, method: 'get' }) }
export function attachKnowledgeDocument(baseId, projectFileId) { return request({ url: `/cost/knowledge/${baseId}/documents`, method: 'post', data: { projectFileId } }) }
export function deleteKnowledgeDocument(id) { return request({ url: `/cost/knowledge/documents/${id}`, method: 'delete' }) }
export function reindexKnowledgeDocument(id) { return request({ url: `/cost/knowledge/documents/${id}/reindex`, method: 'post' }) }
export function listKnowledgeChunks(id, query) { return request({ url: `/cost/knowledge/documents/${id}/chunks`, method: 'get', params: query }) }
export function listKnowledgeProjectOptions() { return request({ url: '/cost/knowledge/project-options', method: 'get' }) }
export function listKnowledgeFileOptions(projectId) { return request({ url: `/cost/knowledge/project/${projectId}/file-options`, method: 'get' }) }
export function queryKnowledge(data) { return request({ url: '/cost/knowledge/query', method: 'post', data }) }
