<template>
  <div v-if="canList" class="boq-manager">
    <el-card shadow="never">
      <el-form :model="batchQuery" inline>
        <el-form-item label="批次名称"><el-input v-model="batchQuery.batchName" clearable @keyup.enter="loadBatches" /></el-form-item>
        <el-form-item label="业务类型">
          <el-select v-model="batchQuery.businessType" clearable style="width: 160px">
            <el-option v-for="dict in cost_boq_business_type" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" icon="Search" @click="searchBatches">查询</el-button></el-form-item>
        <el-form-item><el-button icon="Refresh" @click="resetBatches">重置</el-button></el-form-item>
        <el-form-item v-if="canImport" class="import-action"><el-button type="primary" icon="Upload" @click="importVisible = true">导入清单</el-button></el-form-item>
      </el-form>

      <el-table v-loading="batchLoading" :data="batches" border highlight-current-row @current-change="selectBatch">
        <el-table-column prop="batchName" label="批次名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="业务类型" width="120"><template #default="scope"><dict-tag :options="cost_boq_business_type" :value="scope.row.businessType" /></template></el-table-column>
        <el-table-column prop="sourceFileName" label="来源文件" min-width="170" show-overflow-tooltip />
        <el-table-column prop="sheetName" label="Sheet" width="130" show-overflow-tooltip />
        <el-table-column prop="successCount" label="成功" width="75" align="right" />
        <el-table-column prop="failCount" label="失败" width="75" align="right">
          <template #default="scope"><el-link v-if="scope.row.failCount && canQuery" type="danger" @click.stop="showErrors(scope.row)">{{ scope.row.failCount }}</el-link><span v-else>{{ scope.row.failCount || 0 }}</span></template>
        </el-table-column>
        <el-table-column label="总金额" width="150" align="right"><template #default="scope">{{ formatAmount(scope.row.totalAmount) }}</template></el-table-column>
        <el-table-column label="状态" width="110"><template #default="scope"><dict-tag :options="cost_boq_import_status" :value="scope.row.importStatus" /></template></el-table-column>
        <el-table-column prop="createTime" label="导入时间" width="165" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="scope"><el-button link type="danger" icon="Delete" @click.stop="removeBatch(scope.row)" v-hasPermi="['cost:boq:remove']">删除批次</el-button></template>
        </el-table-column>
      </el-table>
      <pagination v-show="batchTotal > 0" :total="batchTotal" v-model:page="batchQuery.pageNum" v-model:limit="batchQuery.pageSize" @pagination="loadBatches" />
    </el-card>

    <el-card v-if="selectedBatch" shadow="never" class="items-card">
      <template #header><div class="card-header"><span>{{ selectedBatch.batchName }} · 清单明细</span><el-tag type="info">{{ selectedBatch.successCount }} 条</el-tag></div></template>
      <el-form :model="itemQuery" inline>
        <el-form-item label="项目编码"><el-input v-model="itemQuery.itemCode" clearable @keyup.enter="searchItems" /></el-form-item>
        <el-form-item label="项目名称"><el-input v-model="itemQuery.itemName" clearable @keyup.enter="searchItems" /></el-form-item>
        <el-form-item label="专业">
          <el-select v-model="itemQuery.professionalType" clearable style="width: 150px">
            <el-option v-for="dict in cost_professional_type" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" icon="Search" @click="searchItems">查询</el-button></el-form-item>
      </el-form>
      <el-table v-loading="itemLoading" :data="items" border max-height="520">
        <el-table-column prop="itemCode" label="编码" width="150" fixed="left" show-overflow-tooltip />
        <el-table-column prop="itemName" label="名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="itemFeature" label="特征" min-width="240" show-overflow-tooltip />
        <el-table-column prop="unit" label="单位" width="75" />
        <el-table-column prop="quantity" label="工程量" width="120" align="right" />
        <el-table-column prop="unitPrice" label="综合单价" width="120" align="right" />
        <el-table-column prop="totalPrice" label="Excel合价" width="130" align="right" />
        <el-table-column prop="calculatedTotalPrice" label="计算合价" width="130" align="right" />
        <el-table-column label="专业" width="110"><template #default="scope"><dict-tag :options="cost_professional_type" :value="scope.row.professionalType" /></template></el-table-column>
        <el-table-column prop="sourceRow" label="源行" width="75" align="right" />
      </el-table>
      <pagination v-show="itemTotal > 0" :total="itemTotal" v-model:page="itemQuery.pageNum" v-model:limit="itemQuery.pageSize" @pagination="loadItems" />
    </el-card>

    <el-empty v-else description="选择一个导入批次查看清单明细" />

    <el-dialog v-model="importVisible" title="导入工程量清单" width="92%" destroy-on-close append-to-body>
      <project-boq-preview :project-id="projectId" :professional-type="professionalType" @imported="afterImported" />
    </el-dialog>

    <el-drawer v-model="errorVisible" title="导入错误行" size="65%">
      <el-table v-loading="errorLoading" :data="errors" border>
        <el-table-column prop="sourceRow" label="源行" width="80" />
        <el-table-column prop="errorField" label="字段" width="130" />
        <el-table-column prop="rawValue" label="原始值" min-width="170" show-overflow-tooltip />
        <el-table-column prop="errorMessage" label="错误原因" min-width="240" show-overflow-tooltip />
      </el-table>
      <pagination v-show="errorTotal > 0" :total="errorTotal" v-model:page="errorQuery.pageNum" v-model:limit="errorQuery.pageSize" @pagination="loadErrors" />
    </el-drawer>
  </div>
  <el-empty v-else description="暂无工程量清单查看权限" />
</template>

<script setup name="ProjectBoqManager">
import { listBoqBatches, listBoqItems, listBoqErrors, deleteBoqBatch } from '@/api/cost/boq'
import useUserStore from '@/store/modules/user'
import ProjectBoqPreview from './ProjectBoqPreview.vue'

const props = defineProps({ projectId: { type: [Number, String], required: true }, professionalType: { type: String, default: '' } })
const { proxy } = getCurrentInstance()
const userStore = useUserStore()
const { cost_boq_business_type, cost_boq_import_status, cost_professional_type } = useDict(
  'cost_boq_business_type', 'cost_boq_import_status', 'cost_professional_type'
)
const canList = computed(() => userStore.permissions.includes('*:*:*') || userStore.permissions.includes('cost:boq:list'))
const canQuery = computed(() => userStore.permissions.includes('*:*:*') || userStore.permissions.includes('cost:boq:query'))
const canImport = computed(() => userStore.permissions.includes('*:*:*') || (
  userStore.permissions.includes('cost:boq:import') && userStore.permissions.includes('cost:boq:preview') &&
  userStore.permissions.includes('cost:file:upload') && userStore.permissions.includes('cost:file:query')
))
const batchLoading = ref(false)
const itemLoading = ref(false)
const errorLoading = ref(false)
const importVisible = ref(false)
const errorVisible = ref(false)
const batches = ref([])
const items = ref([])
const errors = ref([])
const batchTotal = ref(0)
const itemTotal = ref(0)
const errorTotal = ref(0)
const selectedBatch = ref(null)
const errorBatchId = ref(null)
const batchQuery = reactive({ projectId: props.projectId, batchName: '', businessType: '', pageNum: 1, pageSize: 10 })
const itemQuery = reactive({ batchId: null, itemCode: '', itemName: '', professionalType: '', pageNum: 1, pageSize: 20 })
const errorQuery = reactive({ pageNum: 1, pageSize: 20 })

function loadBatches() {
  if (!canList.value) return
  batchLoading.value = true
  listBoqBatches(batchQuery).then(response => {
    batches.value = response.rows
    batchTotal.value = response.total
    if (selectedBatch.value) {
      const refreshed = response.rows.find(row => row.id === selectedBatch.value.id)
      if (refreshed) selectedBatch.value = refreshed
    }
  }).finally(() => { batchLoading.value = false })
}
function searchBatches() { batchQuery.pageNum = 1; loadBatches() }
function resetBatches() { batchQuery.batchName = ''; batchQuery.businessType = ''; searchBatches() }
function selectBatch(row) {
  if (!row) return
  selectedBatch.value = row
  itemQuery.batchId = row.id
  itemQuery.pageNum = 1
  loadItems()
}
function loadItems() {
  if (!itemQuery.batchId) return
  itemLoading.value = true
  listBoqItems(itemQuery).then(response => { items.value = response.rows; itemTotal.value = response.total }).finally(() => { itemLoading.value = false })
}
function searchItems() { itemQuery.pageNum = 1; loadItems() }
function showErrors(row) { errorBatchId.value = row.id; errorQuery.pageNum = 1; errorVisible.value = true; loadErrors() }
function loadErrors() {
  errorLoading.value = true
  listBoqErrors(errorBatchId.value, errorQuery).then(response => { errors.value = response.rows; errorTotal.value = response.total }).finally(() => { errorLoading.value = false })
}
function removeBatch(row) {
  proxy.$modal.confirm(`删除批次“${row.batchName}”将同步删除其全部清单和错误行，是否继续？`).then(() => deleteBoqBatch(row.id)).then(() => {
    proxy.$modal.msgSuccess('批次已删除')
    if (selectedBatch.value?.id === row.id) { selectedBatch.value = null; items.value = []; itemTotal.value = 0 }
    loadBatches()
  }).catch(() => {})
}
function afterImported(result) {
  importVisible.value = false
  loadBatches()
  if (result.failCount > 0) { errorBatchId.value = result.batchId; errorVisible.value = true; loadErrors() }
}
function formatAmount(value) { return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` }

loadBatches()
</script>

<style scoped lang="scss">
.boq-manager { display: flex; flex-direction: column; gap: 16px; padding-top: 4px; }
.import-action { margin-left: auto; }
.items-card { margin-top: 0; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
