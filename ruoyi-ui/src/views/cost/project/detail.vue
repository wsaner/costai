<template>
  <div class="app-container project-detail-page" v-loading="loading">
    <div class="detail-header">
      <div>
        <el-button link icon="ArrowLeft" @click="router.back()">返回项目列表</el-button>
        <h2>{{ project.projectName || '项目详情' }}</h2>
        <div class="subtitle">{{ project.projectCode || '-' }}</div>
      </div>
      <dict-tag v-if="project.projectStatus" :options="cost_project_status" :value="project.projectStatus" />
    </div>

    <el-tabs v-model="activeTab" class="detail-tabs">
      <el-tab-pane label="项目概况" name="overview">
        <el-card shadow="never">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="项目编号">{{ project.projectCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目名称" :span="2">{{ project.projectName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目类型"><dict-tag :options="cost_project_type" :value="project.projectType" /></el-descriptions-item>
            <el-descriptions-item label="项目专业"><dict-tag :options="cost_professional_type" :value="project.professionalType" /></el-descriptions-item>
            <el-descriptions-item label="项目阶段"><dict-tag :options="cost_project_stage" :value="project.projectStage" /></el-descriptions-item>
            <el-descriptions-item label="项目负责人">{{ project.projectManagerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="地区" :span="2">{{ regionText }}</el-descriptions-item>
            <el-descriptions-item label="建筑面积">{{ areaText }}</el-descriptions-item>
            <el-descriptions-item label="开工日期">{{ project.startDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="竣工日期">{{ project.completionDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="建设单位">{{ project.constructionUnit || '-' }}</el-descriptions-item>
            <el-descriptions-item label="施工单位">{{ project.contractorUnit || '-' }}</el-descriptions-item>
            <el-descriptions-item label="咨询单位">{{ project.consultingUnit || '-' }}</el-descriptions-item>
            <el-descriptions-item label="送审金额">{{ formatAmount(project.submittedAmount) }}</el-descriptions-item>
            <el-descriptions-item label="审定金额">{{ formatAmount(project.approvedAmount) }}</el-descriptions-item>
            <el-descriptions-item label="核增金额">{{ formatAmount(project.increaseAmount) }}</el-descriptions-item>
            <el-descriptions-item label="核减金额">{{ formatAmount(project.reductionAmount) }}</el-descriptions-item>
            <el-descriptions-item label="核减率">{{ formatRate(project.reductionRate) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ parseTime(project.createTime) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="项目描述" :span="3">{{ project.description || '-' }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="3">{{ project.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="项目文件" name="files">
        <project-files v-if="project.id" :project-id="project.id" />
      </el-tab-pane>
      <el-tab-pane label="工程量清单" name="boq">
        <project-boq-manager v-if="project.id" :project-id="project.id" :professional-type="project.professionalType" />
      </el-tab-pane>
      <el-tab-pane label="清单对比" name="compare">
        <project-boq-compare v-if="project.id" :project-id="project.id" />
      </el-tab-pane>
      <el-tab-pane label="审核结果" name="review">
        <project-review-rules v-if="project.id" :project-id="project.id" />
      </el-tab-pane>
      <el-tab-pane v-for="tab in reservedTabs" :key="tab.name" :label="tab.label" :name="tab.name">
        <el-empty :description="`${tab.label}将在后续阶段接入`" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="CostProjectDetail">
import { getProject } from '@/api/cost/project'
import ProjectFiles from './components/ProjectFiles.vue'
import ProjectBoqManager from './components/ProjectBoqManager.vue'
import ProjectBoqCompare from './components/ProjectBoqCompare.vue'
import ProjectReviewRules from './components/ProjectReviewRules.vue'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()
const { cost_project_type, cost_project_stage, cost_project_status, cost_professional_type } = useDict(
  'cost_project_type', 'cost_project_stage', 'cost_project_status', 'cost_professional_type'
)
const loading = ref(false)
const activeTab = ref('overview')
const project = ref({})
const reservedTabs = [
  { name: 'indicator', label: '造价指标' },
  { name: 'material', label: '材料价格' },
  { name: 'ai', label: 'AI分析' },
  { name: 'report', label: '报告' }
]
const regionText = computed(() => [project.value.province, project.value.city, project.value.district].filter(Boolean).join(' / ') || '-')
const areaText = computed(() => project.value.buildingArea == null ? '-' : `${Number(project.value.buildingArea).toLocaleString('zh-CN')} ㎡`)

function formatAmount(value) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}
function formatRate(value) {
  return `${(Number(value || 0) * 100).toFixed(2)}%`
}
function loadProject() {
  loading.value = true
  getProject(route.params.id).then(response => {
    project.value = response.data
  }).catch(() => {
    proxy.$modal.msgError('项目不存在或无权访问')
    router.replace('/cost/project')
  }).finally(() => { loading.value = false })
}

loadProject()
</script>

<style scoped lang="scss">
.project-detail-page { background: #f5f7fa; min-height: calc(100vh - 84px); }
.detail-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 18px; padding: 20px 24px; background: var(--el-bg-color); border-radius: 8px; }
.detail-header h2 { margin: 12px 0 4px; font-size: 24px; }
.subtitle { color: var(--el-text-color-secondary); }
.detail-tabs { padding: 0 20px 20px; background: var(--el-bg-color); border-radius: 8px; }
.detail-tabs :deep(.el-empty) { min-height: 360px; }
</style>
