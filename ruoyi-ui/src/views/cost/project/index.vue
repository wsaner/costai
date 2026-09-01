<template>
  <div class="app-container cost-project-page">
    <el-row :gutter="16" class="overview-row">
      <el-col v-for="item in overviewItems" :key="item.key" :xs="12" :sm="8" class="overview-col">
        <el-card shadow="never" class="overview-card">
          <div class="overview-label">{{ item.label }}</div>
          <div class="overview-value">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="query-card">
      <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch">
        <el-form-item label="项目名称" prop="projectName">
          <el-input v-model="queryParams.projectName" placeholder="请输入项目名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="项目编号" prop="projectCode">
          <el-input v-model="queryParams.projectCode" placeholder="请输入项目编号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="项目类型" prop="projectType">
          <el-select v-model="queryParams.projectType" placeholder="全部类型" clearable>
            <el-option v-for="item in cost_project_type" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目阶段" prop="projectStage">
          <el-select v-model="queryParams.projectStage" placeholder="全部阶段" clearable>
            <el-option v-for="item in cost_project_stage" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目状态" prop="projectStatus">
          <el-select v-model="queryParams.projectStatus" placeholder="全部状态" clearable>
            <el-option v-for="item in cost_project_status" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人" prop="projectManagerId">
          <el-select v-model="queryParams.projectManagerId" placeholder="全部负责人" filterable clearable>
            <el-option v-for="item in managerOptions" :key="item.userId" :label="managerLabel(item)" :value="item.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker v-model="dateRange" value-format="YYYY-MM-DD" type="daterange"
            range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['cost:project:add']">新增项目</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()"
            v-hasPermi="['cost:project:edit']">修改</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()"
            v-hasPermi="['cost:project:remove']">删除</el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="loadData" />
      </el-row>

      <el-table v-loading="loading" :data="projectList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column label="项目编号" prop="projectCode" min-width="130" show-overflow-tooltip />
        <el-table-column label="项目名称" prop="projectName" min-width="180" show-overflow-tooltip>
          <template #default="scope">
            <el-button v-if="canQuery" link type="primary" @click="handleDetail(scope.row)">{{ scope.row.projectName }}</el-button>
            <span v-else>{{ scope.row.projectName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="项目类型" prop="projectType" width="100" align="center">
          <template #default="scope"><dict-tag :options="cost_project_type" :value="scope.row.projectType" /></template>
        </el-table-column>
        <el-table-column label="阶段" prop="projectStage" width="120" align="center">
          <template #default="scope"><dict-tag :options="cost_project_stage" :value="scope.row.projectStage" /></template>
        </el-table-column>
        <el-table-column label="地区" min-width="150" show-overflow-tooltip>
          <template #default="scope">{{ regionText(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="负责人" prop="projectManagerName" width="110" align="center" />
        <el-table-column label="送审金额" prop="submittedAmount" width="130" align="right">
          <template #default="scope">{{ formatAmount(scope.row.submittedAmount) }}</template>
        </el-table-column>
        <el-table-column label="审定金额" prop="approvedAmount" width="130" align="right">
          <template #default="scope">{{ formatAmount(scope.row.approvedAmount) }}</template>
        </el-table-column>
        <el-table-column label="核减金额" prop="reductionAmount" width="130" align="right">
          <template #default="scope"><span class="reduction-amount">{{ formatAmount(scope.row.reductionAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="核减率" prop="reductionRate" width="95" align="right">
          <template #default="scope">{{ formatRate(scope.row.reductionRate) }}</template>
        </el-table-column>
        <el-table-column label="状态" prop="projectStatus" width="120" align="center">
          <template #default="scope">
            <el-select v-if="canEdit" v-model="scope.row.projectStatus" size="small" @change="value => handleStatusChange(scope.row, value)">
              <el-option v-for="item in cost_project_status" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <dict-tag v-else :options="cost_project_status" :value="scope.row.projectStatus" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['cost:project:query']">详情</el-button>
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['cost:project:edit']">修改</el-button>
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['cost:project:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList" />
    </el-card>

    <el-dialog :title="dialogTitle" v-model="open" width="920px" append-to-body destroy-on-close>
      <el-form ref="projectRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="项目编号" prop="projectCode"><el-input v-model="form.projectCode" maxlength="64" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="项目名称" prop="projectName"><el-input v-model="form.projectName" maxlength="200" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="项目类型" prop="projectType"><el-select v-model="form.projectType" style="width:100%"><el-option v-for="item in cost_project_type" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="项目专业" prop="professionalType"><el-select v-model="form.professionalType" style="width:100%"><el-option v-for="item in cost_professional_type" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="项目阶段" prop="projectStage"><el-select v-model="form.projectStage" style="width:100%"><el-option v-for="item in cost_project_stage" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="项目状态" prop="projectStatus"><el-select v-model="form.projectStatus" style="width:100%"><el-option v-for="item in cost_project_status" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="项目负责人" prop="projectManagerId"><el-select v-model="form.projectManagerId" filterable style="width:100%"><el-option v-for="item in managerOptions" :key="item.userId" :label="managerLabel(item)" :value="item.userId" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="建筑面积"><el-input-number v-model="form.buildingArea" :min="0" :precision="4" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="省"><el-input v-model="form.province" maxlength="64" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="市"><el-input v-model="form.city" maxlength="64" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="区/县"><el-input v-model="form.district" maxlength="64" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="送审金额"><el-input-number v-model="form.submittedAmount" :min="0" :precision="2" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="审定金额"><el-input-number v-model="form.approvedAmount" :min="0" :precision="2" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="核增金额"><el-input-number v-model="form.increaseAmount" :min="0" :precision="2" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="核减金额"><el-input-number v-model="form.reductionAmount" :min="0" :precision="2" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="核减率"><el-input :model-value="formatRate(form.reductionRate)" disabled placeholder="保存后由系统计算" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="开工日期"><el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="竣工日期"><el-date-picker v-model="form.completionDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="建设单位"><el-input v-model="form.constructionUnit" maxlength="200" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="施工单位"><el-input v-model="form.contractorUnit" maxlength="200" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="咨询单位"><el-input v-model="form.consultingUnit" maxlength="200" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="项目描述"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="2000" show-word-limit /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="submitting" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CostProject">
import { addProject, changeProjectStatus, deleteProject, getProject, getProjectStatistics, listProject, listProjectManagers, updateProject } from '@/api/cost/project'
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const router = useRouter()
const userStore = useUserStore()
const { cost_project_type, cost_project_stage, cost_project_status, cost_professional_type } = useDict(
  'cost_project_type', 'cost_project_stage', 'cost_project_status', 'cost_professional_type'
)

const loading = ref(false)
const submitting = ref(false)
const showSearch = ref(true)
const open = ref(false)
const dialogTitle = ref('')
const projectList = ref([])
const managerOptions = ref([])
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const dateRange = ref([])
const statistics = ref({ projectCount: 0, submittedAmount: 0, approvedAmount: 0, reductionAmount: 0, averageReductionRate: 0 })

const data = reactive({
  form: {},
  queryParams: { pageNum: 1, pageSize: 10, projectName: undefined, projectCode: undefined, projectType: undefined, projectStage: undefined, projectStatus: undefined, projectManagerId: undefined },
  rules: {
    projectCode: [{ required: true, message: '项目编号不能为空', trigger: 'blur' }],
    projectName: [{ required: true, message: '项目名称不能为空', trigger: 'blur' }],
    projectType: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
    professionalType: [{ required: true, message: '请选择项目专业', trigger: 'change' }],
    projectStage: [{ required: true, message: '请选择项目阶段', trigger: 'change' }],
    projectStatus: [{ required: true, message: '请选择项目状态', trigger: 'change' }],
    projectManagerId: [{ required: true, message: '请选择项目负责人', trigger: 'change' }]
  }
})
const { form, queryParams, rules } = toRefs(data)

const canEdit = computed(() => userStore.permissions.includes('*:*:*') || userStore.permissions.includes('cost:project:edit'))
const canQuery = computed(() => userStore.permissions.includes('*:*:*') || userStore.permissions.includes('cost:project:query'))
const overviewItems = computed(() => [
  { key: 'count', label: '项目总数', value: `${statistics.value.projectCount || 0}` },
  { key: 'submitted', label: '累计送审金额', value: formatAmount(statistics.value.submittedAmount) },
  { key: 'approved', label: '累计审定金额', value: formatAmount(statistics.value.approvedAmount) },
  { key: 'reduction', label: '累计核减金额', value: formatAmount(statistics.value.reductionAmount) },
  { key: 'rate', label: '平均核减率', value: formatRate(statistics.value.averageReductionRate) }
])

function formatAmount(value) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}
function formatRate(value) {
  return `${(Number(value || 0) * 100).toFixed(2)}%`
}
function regionText(row) {
  return [row.province, row.city, row.district].filter(Boolean).join(' / ') || '-'
}
function managerLabel(item) {
  return [item.nickName || item.userName, item.deptName].filter(Boolean).join(' · ')
}
function queryPayload() {
  return proxy.addDateRange({ ...queryParams.value }, dateRange.value)
}
function getList() {
  loading.value = true
  return listProject(queryPayload()).then(response => {
    projectList.value = response.rows
    total.value = response.total
  }).finally(() => { loading.value = false })
}
function getStatistics() {
  const params = queryPayload()
  delete params.pageNum
  delete params.pageSize
  return getProjectStatistics(params).then(response => { statistics.value = response.data })
}
function loadData() {
  return Promise.all([getList(), getStatistics()])
}
function loadManagers() {
  return listProjectManagers().then(response => { managerOptions.value = response.data || [] })
}
function handleQuery() {
  queryParams.value.pageNum = 1
  loadData()
}
function resetQuery() {
  dateRange.value = []
  proxy.resetForm('queryRef')
  handleQuery()
}
function reset() {
  form.value = { id: undefined, projectCode: undefined, projectName: undefined, projectType: undefined,
    professionalType: undefined, projectStage: undefined, projectStatus: undefined, projectManagerId: undefined,
    province: undefined, city: undefined, district: undefined, buildingArea: undefined,
    submittedAmount: 0, approvedAmount: 0, increaseAmount: 0, reductionAmount: 0, reductionRate: 0,
    startDate: undefined, completionDate: undefined, constructionUnit: undefined, contractorUnit: undefined,
    consultingUnit: undefined, description: undefined, remark: undefined }
  proxy.resetForm('projectRef')
}
function cancel() {
  open.value = false
  reset()
}
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = selection.length === 0
}
function handleAdd() {
  reset()
  open.value = true
  dialogTitle.value = '新增造价项目'
}
function handleUpdate(row) {
  reset()
  const id = row?.id || ids.value[0]
  getProject(id).then(response => {
    form.value = response.data
    open.value = true
    dialogTitle.value = '修改造价项目'
  })
}
function handleDetail(row) {
  router.push(`/cost/project-detail/index/${row.id}`)
}
function submitForm() {
  proxy.$refs.projectRef.validate(valid => {
    if (!valid) return
    submitting.value = true
    const request = form.value.id ? updateProject(form.value) : addProject(form.value)
    request.then(() => {
      proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功')
      open.value = false
      loadData()
    }).finally(() => { submitting.value = false })
  })
}
function handleDelete(row) {
  const selectedIds = row?.id || ids.value
  proxy.$modal.confirm(`是否确认删除所选造价项目？`).then(() => deleteProject(selectedIds)).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    loadData()
  }).catch(() => {})
}
function handleStatusChange(row, value) {
  changeProjectStatus({ id: row.id, projectStatus: value }).then(() => {
    proxy.$modal.msgSuccess('状态修改成功')
    loadData()
  }).catch(() => { getList() })
}

loadManagers()
loadData()
</script>

<style scoped lang="scss">
.cost-project-page { background: #f5f7fa; min-height: calc(100vh - 84px); }
.overview-row { margin-bottom: 16px; display: flex; flex-wrap: wrap; }
.overview-col { max-width: 20%; flex: 0 0 20%; }
.overview-card { border: 0; }
.overview-label { color: var(--el-text-color-secondary); font-size: 13px; margin-bottom: 10px; }
.overview-value { color: var(--el-text-color-primary); font-size: 22px; font-weight: 650; white-space: nowrap; }
.query-card { border: 0; }
.reduction-amount { color: var(--el-color-danger); font-weight: 600; }
@media (max-width: 1199px) { .overview-col { max-width: 33.333%; flex: 0 0 33.333%; } }
</style>
