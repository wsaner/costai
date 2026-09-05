<template>
  <div v-if="canList" class="compare-page">
    <el-card shadow="never">
      <el-form :model="pair" inline class="pair-form">
        <el-form-item label="左侧批次">
          <el-select v-model="pair.leftBatchId" filterable placeholder="选择送审/控制价等批次" style="width: 280px" @change="pairChanged">
            <el-option v-for="batch in leftBatchOptions" :key="batch.id" :label="batchLabel(batch)" :value="batch.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="右侧批次">
          <el-select v-model="pair.rightBatchId" filterable placeholder="选择审核/投标等批次" style="width: 280px" @change="pairChanged">
            <el-option v-for="batch in rightBatchOptions" :key="batch.id" :label="batchLabel(batch)" :value="batch.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button v-if="canStart && !hasResults" type="primary" icon="Connection" :loading="matching" :disabled="!pairReady" @click="startMatch">开始匹配</el-button>
          <el-button v-if="canStart && hasResults" type="warning" icon="Refresh" :loading="matching" @click="rematch">重新匹配</el-button>
        </el-form-item>
      </el-form>
      <el-alert title="匹配按项目编码、标准化名称与单位、名称/特征相似度依次执行；重新匹配会保留人工结果。差异均为左值减右值。" type="info" :closable="false" show-icon />
    </el-card>

    <div v-if="pairReady" class="summary-grid">
      <el-card v-for="card in summaryCards" :key="card.label" shadow="never" class="summary-card">
        <div class="summary-label">{{ card.label }}</div>
        <div class="summary-value" :class="card.className">{{ card.value }}</div>
      </el-card>
    </div>

    <el-card v-if="pairReady" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="编码/名称"><el-input v-model="query.keyword" clearable placeholder="搜索左右清单" @keyup.enter="search" /></el-form-item>
        <el-form-item label="匹配状态">
          <el-select v-model="query.matchType" clearable style="width: 160px">
            <el-option v-for="dict in cost_boq_match_type" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" icon="Search" @click="search">查询</el-button></el-form-item>
        <el-form-item><el-button icon="Refresh" @click="resetSearch">重置</el-button></el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" border :row-class-name="rowClass" max-height="620">
        <el-table-column label="左侧清单" fixed="left">
          <el-table-column prop="leftItemCode" label="编码" width="140" show-overflow-tooltip />
          <el-table-column prop="leftItemName" label="名称" min-width="180" show-overflow-tooltip />
        </el-table-column>
        <el-table-column label="右侧清单">
          <el-table-column prop="rightItemCode" label="编码" width="140" show-overflow-tooltip />
          <el-table-column prop="rightItemName" label="名称" min-width="180" show-overflow-tooltip />
        </el-table-column>
        <el-table-column label="工程量">
          <el-table-column prop="leftQuantity" label="左" width="105" align="right" />
          <el-table-column prop="rightQuantity" label="右" width="105" align="right" />
          <el-table-column label="差异" width="115" align="right"><template #default="scope">{{ formatNumber(scope.row.quantityDiff) }}</template></el-table-column>
        </el-table-column>
        <el-table-column label="综合单价">
          <el-table-column prop="leftUnitPrice" label="左" width="105" align="right" />
          <el-table-column prop="rightUnitPrice" label="右" width="105" align="right" />
          <el-table-column label="差异" width="115" align="right"><template #default="scope">{{ formatNumber(scope.row.unitPriceDiff) }}</template></el-table-column>
        </el-table-column>
        <el-table-column label="合价">
          <el-table-column prop="leftTotalPrice" label="左" width="120" align="right" />
          <el-table-column prop="rightTotalPrice" label="右" width="120" align="right" />
          <el-table-column label="差异" width="125" align="right"><template #default="scope">{{ formatNumber(scope.row.totalPriceDiff) }}</template></el-table-column>
        </el-table-column>
        <el-table-column label="匹配状态" width="125" fixed="right">
          <template #default="scope"><dict-tag :options="cost_boq_match_type" :value="scope.row.matchType" /></template>
        </el-table-column>
        <el-table-column label="匹配度" width="90" align="right" fixed="right">
          <template #default="scope">{{ scoreText(scope.row) }}</template>
        </el-table-column>
        <el-table-column v-if="canManual" label="操作" width="145" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openManual(scope.row)">人工匹配</el-button>
            <el-button v-if="isMatched(scope.row)" link type="danger" @click="cancelMatch(scope.row)">取消匹配</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="loadRows" />
      <el-empty v-if="!loading && !hasResults" description="选择两个批次后点击“开始匹配”" />
    </el-card>

    <el-empty v-else description="请先选择两个不同的清单批次" />

    <el-dialog v-model="manualVisible" title="人工指定清单匹配" width="720px" append-to-body destroy-on-close>
      <el-form label-width="110px">
        <el-form-item label="左侧清单" required>
          <el-select v-model="manual.leftItemId" filterable remote reserve-keyword :remote-method="searchLeftItems" :loading="leftLoading" placeholder="输入编码或名称搜索" style="width: 100%">
            <el-option v-for="item in leftItems" :key="item.id" :label="itemLabel(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="右侧清单" required>
          <el-select v-model="manual.rightItemId" filterable remote reserve-keyword :remote-method="searchRightItems" :loading="rightLoading" placeholder="输入编码或名称搜索" style="width: 100%">
            <el-option v-for="item in rightItems" :key="item.id" :label="itemLabel(item)" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualVisible = false">取消</el-button>
        <el-button type="primary" :loading="manualSaving" @click="saveManual">确认匹配</el-button>
      </template>
    </el-dialog>
  </div>
  <el-empty v-else description="暂无清单对比查看权限" />
</template>

<script setup name="ProjectBoqCompare">
import {
  getBoqCompareSummary, listBoqCompares, listCompareBatchOptions, listCompareItemOptions,
  manualMatchBoq, rematchBoqCompare, startBoqCompare, unmatchBoq
} from '@/api/cost/boq'
import useUserStore from '@/store/modules/user'

const props = defineProps({ projectId: { type: [Number, String], required: true } })
const { proxy } = getCurrentInstance()
const userStore = useUserStore()
const { cost_boq_business_type, cost_boq_match_type } = useDict('cost_boq_business_type', 'cost_boq_match_type')
const hasPermission = permission => userStore.permissions.includes('*:*:*') || userStore.permissions.includes(permission)
const canList = computed(() => hasPermission('cost:compare:list'))
const canStart = computed(() => hasPermission('cost:compare:start'))
const canManual = computed(() => hasPermission('cost:compare:manual'))
const batches = ref([])
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const matching = ref(false)
const pair = reactive({ projectId: Number(props.projectId), leftBatchId: null, rightBatchId: null })
const query = reactive({ projectId: Number(props.projectId), leftBatchId: null, rightBatchId: null, keyword: '', matchType: '', pageNum: 1, pageSize: 20 })
const summary = ref(emptySummary())
const pairReady = computed(() => Boolean(pair.leftBatchId && pair.rightBatchId && pair.leftBatchId !== pair.rightBatchId))
const hasResults = computed(() => Number(summary.value.totalCount || 0) > 0)
const leftBatchOptions = computed(() => batches.value.filter(batch => batch.id !== pair.rightBatchId))
const rightBatchOptions = computed(() => batches.value.filter(batch => batch.id !== pair.leftBatchId))
const summaryCards = computed(() => [
  { label: '结果总数', value: summary.value.totalCount || 0 },
  { label: '已匹配', value: summary.value.matchedCount || 0, className: 'success' },
  { label: '精确匹配', value: summary.value.exactCount || 0 },
  { label: '高相似度', value: summary.value.highSimilarityCount || 0 },
  { label: '低相似度', value: summary.value.lowSimilarityCount || 0, className: 'warning' },
  { label: '仅左/仅右', value: `${summary.value.onlyLeftCount || 0} / ${summary.value.onlyRightCount || 0}`, className: 'danger' },
  { label: '人工匹配', value: summary.value.manualCount || 0 },
  { label: '平均匹配度', value: `${(Number(summary.value.averageMatchScore || 0) * 100).toFixed(1)}%` }
])
const manualVisible = ref(false)
const manualSaving = ref(false)
const leftLoading = ref(false)
const rightLoading = ref(false)
const leftItems = ref([])
const rightItems = ref([])
const manual = reactive({ leftItemId: null, rightItemId: null })

function emptySummary() {
  return { totalCount: 0, matchedCount: 0, exactCount: 0, highSimilarityCount: 0, lowSimilarityCount: 0, onlyLeftCount: 0, onlyRightCount: 0, manualCount: 0, averageMatchScore: 0 }
}
function pairParams() { return { projectId: Number(props.projectId), leftBatchId: pair.leftBatchId, rightBatchId: pair.rightBatchId } }
function batchLabel(batch) {
  const type = cost_boq_business_type.value?.find(dict => dict.value === batch.businessType)?.label || batch.businessType
  return `${batch.batchName}（${type}，${batch.successCount || 0}条）`
}
function pairChanged() {
  rows.value = []
  total.value = 0
  summary.value = emptySummary()
  if (!pairReady.value) return
  Object.assign(query, pairParams(), { pageNum: 1 })
  loadSummary()
  loadRows()
}
function loadSummary() {
  if (!pairReady.value) return
  getBoqCompareSummary(pairParams()).then(response => { summary.value = response.data || emptySummary() })
}
function loadRows() {
  if (!pairReady.value) return
  loading.value = true
  listBoqCompares(query).then(response => { rows.value = response.rows; total.value = response.total }).finally(() => { loading.value = false })
}
function search() { query.pageNum = 1; loadRows() }
function resetSearch() { query.keyword = ''; query.matchType = ''; search() }
function startMatch() {
  matching.value = true
  startBoqCompare(pairParams()).then(response => {
    summary.value = response.data
    proxy.$modal.msgSuccess('清单匹配完成')
    query.pageNum = 1
    loadRows()
  }).finally(() => { matching.value = false })
}
function rematch() {
  proxy.$modal.confirm('重新匹配将重建所有自动结果，并保留人工匹配，是否继续？').then(() => {
    matching.value = true
    return rematchBoqCompare(pairParams())
  }).then(response => {
    summary.value = response.data
    proxy.$modal.msgSuccess('重新匹配完成')
    query.pageNum = 1
    loadRows()
  }).catch(() => {}).finally(() => { matching.value = false })
}
function isMatched(row) { return !['ONLY_LEFT', 'ONLY_RIGHT'].includes(row.matchType) }
function rowClass({ row }) {
  if (row.matchType === 'LOW_SIMILARITY') return 'low-similarity-row'
  if (['ONLY_LEFT', 'ONLY_RIGHT'].includes(row.matchType)) return 'unmatched-row'
  return ''
}
function scoreText(row) { return isMatched(row) ? `${(Number(row.matchScore || 0) * 100).toFixed(1)}%` : '-' }
function formatNumber(value) { return value == null ? '-' : Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 8 }) }
function seedItem(side, row) {
  const prefix = side === 'left' ? 'left' : 'right'
  const id = row[`${prefix}ItemId`]
  if (!id) return null
  return { id, itemCode: row[`${prefix}ItemCode`], itemName: row[`${prefix}ItemName`], unit: row[`${prefix}Unit`] }
}
function openManual(row) {
  manual.leftItemId = row.leftItemId || null
  manual.rightItemId = row.rightItemId || null
  leftItems.value = [seedItem('left', row)].filter(Boolean)
  rightItems.value = [seedItem('right', row)].filter(Boolean)
  manualVisible.value = true
  if (!manual.leftItemId) searchLeftItems('')
  if (!manual.rightItemId) searchRightItems('')
}
function searchItems(batchId, keyword, target, loadingRef) {
  loadingRef.value = true
  listCompareItemOptions({ projectId: Number(props.projectId), batchId, keyword, pageNum: 1, pageSize: 50 })
    .then(response => { target.value = response.rows }).finally(() => { loadingRef.value = false })
}
function searchLeftItems(keyword) { searchItems(pair.leftBatchId, keyword, leftItems, leftLoading) }
function searchRightItems(keyword) { searchItems(pair.rightBatchId, keyword, rightItems, rightLoading) }
function itemLabel(item) { return `${item.itemCode || '无编码'} · ${item.itemName || '-'} · ${item.unit || '无单位'}` }
function saveManual() {
  if (!manual.leftItemId || !manual.rightItemId) {
    proxy.$modal.msgError('请选择左右两侧清单')
    return
  }
  manualSaving.value = true
  manualMatchBoq({ ...pairParams(), leftItemId: manual.leftItemId, rightItemId: manual.rightItemId }).then(response => {
    summary.value = response.data
    manualVisible.value = false
    proxy.$modal.msgSuccess('人工匹配已保存')
    loadRows()
  }).finally(() => { manualSaving.value = false })
}
function cancelMatch(row) {
  proxy.$modal.confirm(`确认取消“${row.leftItemName || '-'}”与“${row.rightItemName || '-'}”的匹配？`).then(() => unmatchBoq(row.id)).then(response => {
    summary.value = response.data
    proxy.$modal.msgSuccess('匹配已取消')
    loadRows()
  }).catch(() => {})
}

if (canList.value) {
  listCompareBatchOptions(props.projectId).then(response => { batches.value = response.data || [] })
}
</script>

<style scoped lang="scss">
.compare-page { display: flex; flex-direction: column; gap: 16px; padding-top: 4px; }
.pair-form { margin-bottom: 4px; }
.summary-grid { display: grid; grid-template-columns: repeat(8, minmax(110px, 1fr)); gap: 10px; }
.summary-card :deep(.el-card__body) { padding: 14px 16px; }
.summary-label { color: var(--el-text-color-secondary); font-size: 13px; }
.summary-value { margin-top: 8px; font-size: 22px; font-weight: 600; }
.summary-value.success { color: var(--el-color-success); }
.summary-value.warning { color: var(--el-color-warning); }
.summary-value.danger { color: var(--el-color-danger); }
:deep(.low-similarity-row) { --el-table-tr-bg-color: var(--el-color-warning-light-9); }
:deep(.unmatched-row) { --el-table-tr-bg-color: var(--el-color-danger-light-9); }
@media (max-width: 1400px) { .summary-grid { grid-template-columns: repeat(4, 1fr); } }
</style>
