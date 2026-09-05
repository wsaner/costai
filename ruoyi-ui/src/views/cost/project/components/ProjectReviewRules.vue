<template>
  <div v-if="canList" class="review-page">
    <el-card shadow="never">
      <el-form :model="pair" inline>
        <el-form-item label="左侧批次">
          <el-select v-model="pair.leftBatchId" filterable placeholder="选择左侧批次" style="width: 240px">
            <el-option v-for="batch in leftOptions" :key="batch.id" :label="batchLabel(batch)" :value="batch.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="右侧批次">
          <el-select v-model="pair.rightBatchId" filterable placeholder="选择右侧批次" style="width: 240px">
            <el-option v-for="batch in rightOptions" :key="batch.id" :label="batchLabel(batch)" :value="batch.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务名称"><el-input v-model="pair.taskName" maxlength="200" clearable placeholder="不填则按左右批次生成" style="width: 220px" /></el-form-item>
        <el-form-item>
          <el-button v-if="canStart" type="primary" icon="DataAnalysis" :loading="reviewing" :disabled="!pairReady" @click="startReview">执行规则审核</el-button>
          <el-button v-if="canConfig" icon="Setting" @click="openConfigs">规则配置</el-button>
        </el-form-item>
      </el-form>
      <el-alert title="规则审核使用Java确定性计算，不调用AI。执行前请先在“清单对比”完成所选批次匹配。" type="info" :closable="false" show-icon />
    </el-card>

    <el-card shadow="never">
      <template #header><span>审核任务</span></template>
      <el-table v-loading="taskLoading" :data="tasks" border highlight-current-row @current-change="selectTask">
        <el-table-column prop="taskName" label="任务名称" min-width="190" show-overflow-tooltip />
        <el-table-column prop="leftBatchName" label="左侧批次" min-width="135" show-overflow-tooltip />
        <el-table-column prop="rightBatchName" label="右侧批次" min-width="135" show-overflow-tooltip />
        <el-table-column label="状态" width="90"><template #default="scope"><dict-tag :options="cost_review_status" :value="scope.row.status" /></template></el-table-column>
        <el-table-column prop="issueCount" label="问题数" width="85" align="right" />
        <el-table-column label="高风险" width="85" align="right"><template #default="scope">{{ (scope.row.highCount || 0) + (scope.row.criticalCount || 0) }}</template></el-table-column>
        <el-table-column prop="riskAmount" label="风险金额" width="130" align="right"><template #default="scope">{{ money(scope.row.riskAmount) }}</template></el-table-column>
        <el-table-column prop="startedBy" label="执行人" width="95" />
        <el-table-column prop="startTime" label="执行时间" width="165"><template #default="scope">{{ parseTime(scope.row.startTime) }}</template></el-table-column>
      </el-table>
      <pagination v-show="taskTotal > 0" :total="taskTotal" v-model:page="taskQuery.pageNum" v-model:limit="taskQuery.pageSize" @pagination="loadTasks" />
    </el-card>

    <template v-if="selectedTask">
      <div class="summary-grid">
        <el-card v-for="card in summaryCards" :key="card.label" shadow="never">
          <div class="summary-label">{{ card.label }}</div>
          <div class="summary-value" :class="card.className">{{ card.value }}</div>
        </el-card>
      </div>
      <el-card shadow="never">
        <el-form :model="issueQuery" inline>
          <el-form-item label="编码/名称"><el-input v-model="issueQuery.keyword" clearable placeholder="搜索清单或问题" @keyup.enter="searchIssues" /></el-form-item>
          <el-form-item label="问题类型"><el-select v-model="issueQuery.issueType" clearable style="width: 160px"><el-option v-for="dict in issueTypeOptions" :key="dict.value" :label="dict.label" :value="dict.value" /></el-select></el-form-item>
          <el-form-item label="风险级别"><el-select v-model="issueQuery.issueLevel" clearable style="width: 125px"><el-option v-for="dict in cost_issue_level" :key="dict.value" :label="dict.label" :value="dict.value" /></el-select></el-form-item>
          <el-form-item label="状态"><el-select v-model="issueQuery.status" clearable style="width: 125px"><el-option v-for="dict in cost_issue_status" :key="dict.value" :label="dict.label" :value="dict.value" /></el-select></el-form-item>
          <el-form-item><el-button type="primary" icon="Search" @click="searchIssues">查询</el-button></el-form-item>
          <el-form-item><el-button icon="Refresh" @click="resetIssues">重置</el-button></el-form-item>
        </el-form>
        <el-table v-loading="issueLoading" :data="issues" border :row-class-name="issueRowClass" max-height="580" @row-dblclick="openIssue">
          <el-table-column label="级别" width="90"><template #default="scope"><dict-tag :options="cost_issue_level" :value="scope.row.issueLevel" /></template></el-table-column>
          <el-table-column label="问题类型" width="125"><template #default="scope"><dict-tag :options="cost_review_issue_type" :value="scope.row.issueType" /></template></el-table-column>
          <el-table-column prop="itemCodeSnapshot" label="清单编码" width="135" show-overflow-tooltip />
          <el-table-column prop="itemNameSnapshot" label="清单名称" min-width="165" show-overflow-tooltip />
          <el-table-column prop="issueTitle" label="问题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="originalValue" label="原值/左值" width="120" show-overflow-tooltip />
          <el-table-column prop="referenceValue" label="参考/右值" width="120" show-overflow-tooltip />
          <el-table-column label="差异率" width="90" align="right"><template #default="scope">{{ rate(scope.row.differenceRate) }}</template></el-table-column>
          <el-table-column label="风险金额" width="120" align="right"><template #default="scope">{{ money(scope.row.riskAmount) }}</template></el-table-column>
          <el-table-column label="状态" width="95"><template #default="scope"><dict-tag :options="cost_issue_status" :value="scope.row.status" /></template></el-table-column>
          <el-table-column v-if="canQuery" label="操作" width="90" fixed="right"><template #default="scope"><el-button link type="primary" @click="openIssue(scope.row)">查看处理</el-button></template></el-table-column>
        </el-table>
        <pagination v-show="issueTotal > 0" :total="issueTotal" v-model:page="issueQuery.pageNum" v-model:limit="issueQuery.pageSize" @pagination="loadIssues" />
        <el-empty v-if="!issueLoading && issueTotal === 0" description="该审核任务未发现符合条件的问题" />
      </el-card>
    </template>

    <el-drawer v-model="issueDrawer" title="审核问题详情" size="620px" append-to-body destroy-on-close>
      <div v-loading="issueDetailLoading" class="issue-detail">
        <template v-if="currentIssue">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="问题类型"><dict-tag :options="cost_review_issue_type" :value="currentIssue.issueType" /></el-descriptions-item>
            <el-descriptions-item label="命中规则">{{ currentIssue.ruleCode }}</el-descriptions-item>
            <el-descriptions-item label="清单编码">{{ currentIssue.itemCodeSnapshot || '-' }}</el-descriptions-item>
            <el-descriptions-item label="清单名称">{{ currentIssue.itemNameSnapshot || '-' }}</el-descriptions-item>
            <el-descriptions-item label="左清单ID">{{ currentIssue.leftItemId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="右清单ID">{{ currentIssue.rightItemId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="原值/左值">{{ currentIssue.originalValue || '-' }}</el-descriptions-item>
            <el-descriptions-item label="参考/右值">{{ currentIssue.referenceValue || '-' }}</el-descriptions-item>
            <el-descriptions-item label="差异值">{{ currentIssue.differenceValue ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="差异率">{{ rate(currentIssue.differenceRate) }}</el-descriptions-item>
            <el-descriptions-item label="风险金额">{{ money(currentIssue.riskAmount) }}</el-descriptions-item>
            <el-descriptions-item label="当前状态"><dict-tag :options="cost_issue_status" :value="currentIssue.status" /></el-descriptions-item>
          </el-descriptions>
          <el-alert class="detail-block" :title="currentIssue.issueTitle" :description="currentIssue.issueDescription" type="warning" :closable="false" show-icon />
          <el-collapse class="detail-block">
            <el-collapse-item title="查看规则证据" name="evidence"><pre class="evidence">{{ evidenceText }}</pre></el-collapse-item>
          </el-collapse>
          <el-card class="detail-block" shadow="never">
            <template #header>
              <div class="ai-header">
                <span>AI语义分析（仅供人工复核）</span>
                <el-button v-if="canAi" type="primary" plain :loading="aiAnalyzing"
                  :disabled="!currentIssue.aiEligible" @click="runAiAnalysis">
                  {{ currentIssue.aiAnalyzedTime ? '重新分析' : '开始分析' }}
                </el-button>
              </div>
            </template>
            <el-alert v-if="!currentIssue.aiEligible" :title="currentIssue.aiEligibilityReason || '该问题不适合AI语义复核'"
              type="info" :closable="false" show-icon />
            <template v-else-if="currentIssue.aiAnalyzedTime">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="AI判断">
                  <el-tag :type="currentIssue.aiHasIssue === 'Y' ? 'danger' : 'success'">
                    {{ currentIssue.aiHasIssue === 'Y' ? '疑似存在问题' : '未发现语义问题' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="置信度">{{ confidence(currentIssue.aiConfidence) }}</el-descriptions-item>
                <el-descriptions-item label="建议类型"><dict-tag :options="cost_review_issue_type" :value="currentIssue.aiIssueType" /></el-descriptions-item>
                <el-descriptions-item label="建议风险"><dict-tag :options="cost_issue_level" :value="currentIssue.aiIssueLevel" /></el-descriptions-item>
                <el-descriptions-item label="分析模型">{{ currentIssue.aiModel || '-' }}</el-descriptions-item>
                <el-descriptions-item label="分析时间">{{ parseTime(currentIssue.aiAnalyzedTime) }}</el-descriptions-item>
              </el-descriptions>
              <div class="ai-copy"><strong>{{ currentIssue.aiTitle }}</strong></div>
              <div class="ai-copy"><span class="copy-label">分析：</span>{{ currentIssue.aiAnalysis }}</div>
              <div class="ai-copy"><span class="copy-label">建议：</span>{{ currentIssue.aiSuggestion }}</div>
              <el-button v-if="canHandle" class="adopt-button" text type="primary" @click="adoptAiSuggestion">带入人工处理表单</el-button>
            </template>
            <el-empty v-else description="尚未进行AI语义分析；只会发送当前问题及左右各一条清单" :image-size="64" />
          </el-card>
          <el-form v-if="canHandle" label-position="top" class="handle-form">
            <el-form-item label="风险等级"><el-select v-model="handleForm.issueLevel" style="width: 100%"><el-option v-for="dict in cost_issue_level" :key="dict.value" :label="dict.label" :value="dict.value" /></el-select></el-form-item>
            <el-form-item label="审核意见（必填）"><el-input v-model="handleForm.reviewComment" type="textarea" :rows="4" maxlength="2000" show-word-limit placeholder="填写确认依据、忽略原因或整改说明" /></el-form-item>
          </el-form>
          <el-descriptions v-else-if="currentIssue.reviewComment" class="detail-block" :column="1" border>
            <el-descriptions-item label="处理人">{{ currentIssue.reviewer || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核意见">{{ currentIssue.reviewComment }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </div>
      <template #footer>
        <div v-if="canHandle && currentIssue" class="drawer-actions">
          <el-button :loading="handling" @click="submitHandle(currentIssue.status)">仅保存等级/意见</el-button>
          <el-button type="danger" plain :loading="handling" @click="submitHandle('IGNORED')">忽略</el-button>
          <el-button type="primary" :loading="handling" @click="submitHandle('CONFIRMED')">确认问题</el-button>
          <el-button v-if="currentIssue.status === 'CONFIRMED'" type="success" :loading="handling" @click="submitHandle('RECTIFIED')">标记已整改</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="configVisible" title="审核规则配置" width="900px" append-to-body>
      <el-alert title="比例值使用小数，例如0.10表示10%。修改只影响之后新执行的审核任务，历史任务保留原配置快照。" type="warning" :closable="false" show-icon />
      <el-table v-loading="configLoading" :data="configs" border max-height="560" style="margin-top: 14px">
        <el-table-column prop="ruleName" label="规则" width="145" />
        <el-table-column prop="configName" label="配置" width="145" />
        <el-table-column label="值" min-width="220"><template #default="scope"><el-select v-if="scope.row.valueType === 'BOOLEAN'" v-model="configValues[scope.row.id]" style="width: 100%"><el-option label="启用" value="true" /><el-option label="停用" value="false" /></el-select><el-input v-else v-model="configValues[scope.row.id]" /></template></el-table-column>
        <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
        <el-table-column label="操作" width="85"><template #default="scope"><el-button link type="primary" :loading="savingConfigId === scope.row.id" @click="saveConfig(scope.row)">保存</el-button></template></el-table-column>
      </el-table>
    </el-dialog>
  </div>
  <el-empty v-else description="暂无造价审核查看权限" />
</template>

<script setup name="ProjectReviewRules">
import { listCompareBatchOptions } from '@/api/cost/boq'
import { analyzeReviewIssue, getReviewIssue, getReviewTask, handleReviewIssue, listReviewIssues, listReviewRuleConfigs, listReviewTasks, startCostReview, updateReviewRuleConfig } from '@/api/cost/review'
import useUserStore from '@/store/modules/user'

const props = defineProps({ projectId: { type: [Number, String], required: true } })
const { proxy } = getCurrentInstance()
const userStore = useUserStore()
const { cost_boq_business_type, cost_review_status, cost_review_issue_type, cost_issue_level, cost_issue_status } = useDict('cost_boq_business_type', 'cost_review_status', 'cost_review_issue_type', 'cost_issue_level', 'cost_issue_status')
const hasPermission = permission => userStore.permissions.includes('*:*:*') || userStore.permissions.includes(permission)
const canList = computed(() => hasPermission('cost:review:list'))
const canQuery = computed(() => hasPermission('cost:review:query'))
const canStart = computed(() => hasPermission('cost:review:start'))
const canConfig = computed(() => hasPermission('cost:review:config'))
const canHandle = computed(() => hasPermission('cost:review:handle'))
const canAi = computed(() => hasPermission('cost:review:ai'))
const allowedIssueTypes = new Set(['QUANTITY', 'UNIT_PRICE', 'TOTAL_PRICE', 'DUPLICATE', 'MISSING', 'NEW_ITEM', 'FEATURE', 'DATA', 'WRONG_ITEM', 'OTHER'])
const issueTypeOptions = computed(() => (cost_review_issue_type.value || []).filter(item => allowedIssueTypes.has(item.value)))
const pair = reactive({ projectId: Number(props.projectId), leftBatchId: null, rightBatchId: null, taskName: '' })
const batchOptions = ref([])
const leftOptions = computed(() => batchOptions.value.filter(item => item.id !== pair.rightBatchId))
const rightOptions = computed(() => batchOptions.value.filter(item => item.id !== pair.leftBatchId))
const pairReady = computed(() => Boolean(pair.leftBatchId && pair.rightBatchId && pair.leftBatchId !== pair.rightBatchId))
const reviewing = ref(false)
const taskLoading = ref(false)
const tasks = ref([])
const taskTotal = ref(0)
const taskQuery = reactive({ projectId: Number(props.projectId), pageNum: 1, pageSize: 10 })
const selectedTask = ref(null)
const issueLoading = ref(false)
const issues = ref([])
const issueTotal = ref(0)
const issueQuery = reactive({ keyword: '', issueType: '', issueLevel: '', status: '', pageNum: 1, pageSize: 20 })
const summaryCards = computed(() => {
  const value = selectedTask.value || {}
  return [
    { label: '问题数量', value: value.issueCount || 0 },
    { label: '高风险', value: (value.highCount || 0) + (value.criticalCount || 0), className: 'danger' },
    { label: '核减金额', value: money(value.reductionAmount), className: 'danger' },
    { label: '工程量异常', value: value.quantityIssueCount || 0, className: 'warning' },
    { label: '单价异常', value: value.unitPriceIssueCount || 0, className: 'warning' },
    { label: '漏项', value: value.missingIssueCount || 0, className: 'warning' },
    { label: '重复项', value: value.duplicateIssueCount || 0, className: 'warning' }
  ]
})
const issueDrawer = ref(false)
const issueDetailLoading = ref(false)
const currentIssue = ref(null)
const handleForm = reactive({ issueLevel: '', reviewComment: '' })
const handling = ref(false)
const aiAnalyzing = ref(false)
const evidenceText = computed(() => {
  if (!currentIssue.value?.evidenceJson) return '无规则证据'
  try { return JSON.stringify(JSON.parse(currentIssue.value.evidenceJson), null, 2) } catch { return currentIssue.value.evidenceJson }
})
const configVisible = ref(false)
const configLoading = ref(false)
const configs = ref([])
const configValues = reactive({})
const savingConfigId = ref(null)

function batchLabel(batch) {
  const type = cost_boq_business_type.value?.find(item => item.value === batch.businessType)?.label || batch.businessType
  return `${batch.batchName}（${type}，${batch.successCount || 0}条）`
}
function loadTasks(selectId) {
  taskLoading.value = true
  listReviewTasks(taskQuery).then(response => {
    tasks.value = response.rows
    taskTotal.value = response.total
    if (selectId) {
      const target = tasks.value.find(item => item.id === selectId)
      if (target) selectTask(target)
    }
  }).finally(() => { taskLoading.value = false })
}
function startReview() {
  reviewing.value = true
  startCostReview(pair).then(response => {
    proxy.$modal.msgSuccess(`审核任务完成，发现${response.data.issueCount || 0}个问题`)
    taskQuery.pageNum = 1
    pair.taskName = ''
    loadTasks(response.data.id)
  }).finally(() => { reviewing.value = false })
}
function selectTask(row) {
  if (!row) return
  selectedTask.value = row
  issueQuery.pageNum = 1
  loadIssues()
}
function loadIssues() {
  if (!selectedTask.value) return
  issueLoading.value = true
  listReviewIssues(selectedTask.value.id, issueQuery).then(response => {
    issues.value = response.rows
    issueTotal.value = response.total
  }).finally(() => { issueLoading.value = false })
}
function refreshSelectedTask() {
  if (!selectedTask.value) return
  getReviewTask(selectedTask.value.id).then(response => {
    selectedTask.value = response.data
    const index = tasks.value.findIndex(item => item.id === response.data.id)
    if (index >= 0) tasks.value[index] = response.data
  })
}
function searchIssues() { issueQuery.pageNum = 1; loadIssues() }
function resetIssues() { Object.assign(issueQuery, { keyword: '', issueType: '', issueLevel: '', status: '', pageNum: 1 }); loadIssues() }
function openIssue(row) {
  if (!canQuery.value) return
  issueDrawer.value = true
  issueDetailLoading.value = true
  getReviewIssue(row.id).then(response => {
    currentIssue.value = response.data
    handleForm.issueLevel = response.data.issueLevel
    handleForm.reviewComment = response.data.reviewComment || ''
  }).finally(() => { issueDetailLoading.value = false })
}
function submitHandle(status) {
  if (!handleForm.reviewComment.trim()) return proxy.$modal.msgWarning('请先填写审核意见')
  handling.value = true
  handleReviewIssue(currentIssue.value.id, { status, issueLevel: handleForm.issueLevel, reviewComment: handleForm.reviewComment }).then(response => {
    currentIssue.value = response.data
    handleForm.issueLevel = response.data.issueLevel
    proxy.$modal.msgSuccess('审核问题已更新')
    loadIssues()
    refreshSelectedTask()
  }).finally(() => { handling.value = false })
}
function runAiAnalysis() {
  if (!currentIssue.value?.aiEligible) return
  aiAnalyzing.value = true
  analyzeReviewIssue(currentIssue.value.id).then(response => {
    currentIssue.value = response.data.issue
    proxy.$modal.msgSuccess('AI语义分析完成，请人工复核后处理')
    loadIssues()
  }).finally(() => { aiAnalyzing.value = false })
}
function adoptAiSuggestion() {
  if (!currentIssue.value?.aiAnalyzedTime) return
  handleForm.issueLevel = currentIssue.value.aiIssueLevel || handleForm.issueLevel
  if (!handleForm.reviewComment.trim()) {
    handleForm.reviewComment = `AI建议（待人工核实）：${currentIssue.value.aiSuggestion || ''}`.slice(0, 2000)
  }
  proxy.$modal.msgSuccess('已带入表单，保存前请人工核实并修改')
}
function rate(value) { return value == null ? '-' : `${(Math.abs(Number(value)) * 100).toFixed(2)}%` }
function confidence(value) { return value == null ? '-' : `${(Number(value) * 100).toFixed(1)}%` }
function money(value) { return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` }
function issueRowClass({ row }) { return ['HIGH', 'CRITICAL'].includes(row.issueLevel) ? 'high-risk-row' : row.issueLevel === 'MEDIUM' ? 'medium-risk-row' : '' }
function openConfigs() {
  configVisible.value = true
  configLoading.value = true
  listReviewRuleConfigs().then(response => {
    configs.value = response.data || []
    configs.value.forEach(item => { configValues[item.id] = item.configValue })
  }).finally(() => { configLoading.value = false })
}
function saveConfig(config) {
  savingConfigId.value = config.id
  updateReviewRuleConfig(config.id, configValues[config.id]).then(() => {
    config.configValue = configValues[config.id]
    proxy.$modal.msgSuccess('规则配置已保存')
  }).finally(() => { savingConfigId.value = null })
}

if (canList.value) {
  listCompareBatchOptions(props.projectId).then(response => { batchOptions.value = response.data || [] })
  loadTasks()
}
</script>

<style scoped lang="scss">
.review-page { display: flex; flex-direction: column; gap: 16px; padding-top: 4px; }
.summary-grid { display: grid; grid-template-columns: repeat(7, minmax(115px, 1fr)); gap: 12px; }
.summary-grid :deep(.el-card__body) { padding: 14px 16px; }
.summary-label { color: var(--el-text-color-secondary); font-size: 13px; }
.summary-value { margin-top: 8px; font-size: 21px; font-weight: 600; }
.summary-value.warning { color: var(--el-color-warning); }
.summary-value.danger { color: var(--el-color-danger); }
:deep(.high-risk-row) { --el-table-tr-bg-color: var(--el-color-danger-light-9); }
:deep(.medium-risk-row) { --el-table-tr-bg-color: var(--el-color-warning-light-9); }
.detail-block { margin-top: 16px; }
.handle-form { margin-top: 18px; }
.evidence { margin: 0; padding: 12px; max-height: 260px; overflow: auto; background: var(--el-fill-color-light); white-space: pre-wrap; word-break: break-all; }
.drawer-actions { display: flex; justify-content: flex-end; gap: 8px; }
.ai-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.ai-copy { margin-top: 12px; line-height: 1.7; white-space: pre-wrap; word-break: break-word; }
.copy-label { color: var(--el-text-color-secondary); }
.adopt-button { margin-top: 8px; }
@media (max-width: 1500px) { .summary-grid { grid-template-columns: repeat(4, 1fr); } }
</style>
