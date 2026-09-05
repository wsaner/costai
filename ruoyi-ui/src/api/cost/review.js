import request from '@/utils/request'

export function startCostReview(data) {
  return request({ url: '/cost/review/tasks', method: 'post', data, timeout: 300000 })
}

export function listReviewTasks(params) {
  return request({ url: '/cost/review/tasks', method: 'get', params })
}

export function getReviewTask(reviewTaskId) {
  return request({ url: `/cost/review/tasks/${reviewTaskId}`, method: 'get' })
}

export function listReviewIssues(reviewTaskId, params) {
  return request({ url: `/cost/review/tasks/${reviewTaskId}/issues`, method: 'get', params })
}

export function getReviewIssue(issueId) {
  return request({ url: `/cost/review/issues/${issueId}`, method: 'get' })
}

export function handleReviewIssue(issueId, data) {
  return request({ url: `/cost/review/issues/${issueId}`, method: 'put', data })
}

export function analyzeReviewIssue(issueId, data = {}) {
  return request({ url: `/cost/review/issues/${issueId}/ai-analysis`, method: 'post', data, timeout: 120000 })
}

export function listReviewRuleConfigs() {
  return request({ url: '/cost/review/rule-configs', method: 'get' })
}

export function updateReviewRuleConfig(configId, configValue) {
  return request({ url: `/cost/review/rule-configs/${configId}`, method: 'put', data: { configValue } })
}
