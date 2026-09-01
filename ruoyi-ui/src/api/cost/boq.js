import request from '@/utils/request'

export function uploadBoqPreview(projectId, file) {
  const data = new FormData()
  data.append('file', file)
  return request({
    url: `/cost/boq/preview/upload/${projectId}`,
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false },
    timeout: 120000
  })
}

export function previewProjectBoqFile(projectFileId, sheetName) {
  return request({
    url: `/cost/boq/preview/files/${projectFileId}`,
    method: 'post',
    data: { sheetName },
    timeout: 120000
  })
}

export function importBoq(projectId, data) {
  return request({
    url: `/cost/boq/imports/${projectId}`,
    method: 'post',
    data,
    timeout: 300000
  })
}

export function listBoqBatches(params) {
  return request({ url: '/cost/boq/batches', method: 'get', params })
}

export function getBoqBatch(batchId) {
  return request({ url: `/cost/boq/batches/${batchId}`, method: 'get' })
}

export function listBoqItems(params) {
  return request({ url: '/cost/boq/items', method: 'get', params })
}

export function listBoqErrors(batchId, params) {
  return request({ url: `/cost/boq/batches/${batchId}/errors`, method: 'get', params })
}

export function deleteBoqBatch(batchId) {
  return request({ url: `/cost/boq/batches/${batchId}`, method: 'delete' })
}
