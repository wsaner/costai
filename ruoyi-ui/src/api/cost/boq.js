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

export function startBoqCompare(data) {
  return request({ url: '/cost/boq/compares', method: 'post', data, timeout: 300000 })
}

export function rematchBoqCompare(data) {
  return request({ url: '/cost/boq/compares/rematch', method: 'post', data, timeout: 300000 })
}

export function listBoqCompares(params) {
  return request({ url: '/cost/boq/compares', method: 'get', params })
}

export function getBoqCompareSummary(params) {
  return request({ url: '/cost/boq/compares/summary', method: 'get', params })
}

export function manualMatchBoq(data) {
  return request({ url: '/cost/boq/compares/manual', method: 'put', data })
}

export function unmatchBoq(compareId) {
  return request({ url: `/cost/boq/compares/${compareId}/unmatch`, method: 'put' })
}

export function listCompareBatchOptions(projectId) {
  return request({ url: `/cost/boq/compares/batch-options/${projectId}`, method: 'get' })
}

export function listCompareItemOptions(params) {
  return request({ url: '/cost/boq/compares/item-options', method: 'get', params })
}
