<template>
  <div class="boq-preview">
    <el-card shadow="never">
      <el-steps :active="activeStep" align-center finish-status="success">
        <el-step title="上传文件" description="选择常见造价 Excel 或 CSV" />
        <el-step title="字段映射" description="确认系统识别结果" />
        <el-step title="数据确认" description="核对前 50 条数据" />
      </el-steps>
    </el-card>

    <el-card v-if="activeStep === 0" shadow="never" v-loading="loading" class="step-card">
      <div class="step-heading">
        <div>
          <h3>上传工程量清单</h3>
          <p>文件将同时进入当前项目的“项目文件”，系统只做预览识别，不会写入清单主表。</p>
        </div>
      </div>
      <el-upload
        v-if="canUpload"
        ref="uploadRef"
        v-model:file-list="fileList"
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls,.csv"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        :on-exceed="handleExceed"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到这里，或<em>点击选择</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 xlsx、xls、csv，单文件不超过 10 MB；不会假设表头位于第一行。</div>
        </template>
      </el-upload>
      <div class="step-actions">
        <el-button v-if="canUpload" type="primary" :loading="loading" :disabled="!selectedFile" @click="startPreview">
          读取并识别字段
        </el-button>
      </div>
    </el-card>

    <el-card v-else-if="activeStep === 1" shadow="never" v-loading="loading" class="step-card">
      <div class="mapping-toolbar">
        <div>
          <h3>确认字段映射</h3>
          <p>检测到表头位于第 {{ preview.detectedHeaderRow }} 行，共识别 {{ recognizedCount }} 个标准字段。</p>
        </div>
        <el-select v-model="selectedSheet" style="width: 240px" @change="changeSheet">
          <el-option
            v-for="sheet in preview.sheets"
            :key="sheet.index"
            :label="sheetLabel(sheet)"
            :value="sheet.name"
          />
        </el-select>
      </div>

      <el-alert v-for="warning in displayWarnings" :key="warning" :title="warning" type="warning" :closable="false" show-icon class="warning" />

      <el-table :data="preview.columns" border>
        <el-table-column prop="key" label="Excel列" width="90" align="center" />
        <el-table-column prop="header" label="识别到的列名" min-width="190" show-overflow-tooltip />
        <el-table-column label="样例值" min-width="260" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.sampleValues.join('；') || '-' }}</template>
        </el-table-column>
        <el-table-column label="映射到标准字段" width="220">
          <template #default="scope">
            <el-select v-model="columnMappings[scope.row.key]" clearable placeholder="不导入该列">
              <el-option
                v-for="field in preview.standardFields"
                :key="field.code"
                :label="field.label"
                :value="field.code"
                :disabled="isFieldUsed(field.code, scope.row.key)"
              />
            </el-select>
          </template>
        </el-table-column>
      </el-table>

      <div class="step-actions between">
        <el-button @click="activeStep = 0">重新选择文件</el-button>
        <el-button type="primary" @click="confirmMapping">下一步：数据确认</el-button>
      </div>
    </el-card>

    <el-card v-else shadow="never" class="step-card">
      <div class="step-heading">
        <div>
          <h3>确认预览数据</h3>
          <p>当前展示 {{ preview.previewRows.length }} 条样本；确认后将按当前映射全量流式导入。</p>
        </div>
        <el-tag type="info">{{ preview.fileName }} / {{ preview.selectedSheet }}</el-tag>
      </div>

      <el-alert v-for="warning in displayWarnings" :key="warning" :title="warning" type="warning" :closable="false" show-icon class="warning" />

      <el-table :data="preview.previewRows" border max-height="560">
        <el-table-column type="index" label="#" width="60" fixed="left" />
        <el-table-column
          v-for="column in mappedColumns"
          :key="column.key"
          :prop="column.key"
          :label="mappedFieldLabel(column.key)"
          min-width="150"
          show-overflow-tooltip
        >
          <template #header>
            <div class="mapped-header">
              <span>{{ mappedFieldLabel(column.key) }}</span>
              <small>{{ column.key }} · {{ column.header }}</small>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="mappedColumns.length === 0" description="尚未选择需要确认的字段" />
      <el-form :model="importForm" label-width="100px" class="import-form">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="批次名称" required>
              <el-input v-model="importForm.batchName" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="业务类型" required>
              <el-select v-model="importForm.businessType" style="width: 100%">
                <el-option v-for="dict in cost_boq_business_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="专业">
              <el-select v-model="importForm.professionalType" clearable style="width: 100%">
                <el-option v-for="dict in cost_professional_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div class="step-actions between">
        <el-button @click="activeStep = 1">返回修改映射</el-button>
        <el-button v-if="canImport" type="primary" :loading="importing" @click="executeImport">确认并正式导入</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup name="ProjectBoqPreview">
import { uploadBoqPreview, previewProjectBoqFile, importBoq } from '@/api/cost/boq'
import useUserStore from '@/store/modules/user'

const props = defineProps({
  projectId: { type: [Number, String], required: true },
  professionalType: { type: String, default: '' }
})
const emit = defineEmits(['imported'])
const { proxy } = getCurrentInstance()
const { cost_boq_business_type, cost_professional_type } = useDict('cost_boq_business_type', 'cost_professional_type')
const userStore = useUserStore()
const activeStep = ref(0)
const loading = ref(false)
const importing = ref(false)
const fileList = ref([])
const selectedFile = ref(null)
const preview = ref({ sheets: [], columns: [], previewRows: [], mappingSuggestions: {}, standardFields: [], warnings: [] })
const selectedSheet = ref('')
const columnMappings = reactive({})
const importForm = reactive({ batchName: '', businessType: 'SUBMITTED', professionalType: props.professionalType })

const recognizedCount = computed(() => Object.values(columnMappings).filter(Boolean).length)
const mappedColumns = computed(() => preview.value.columns.filter(column => columnMappings[column.key]))
const displayWarnings = computed(() => {
  const warnings = [...(preview.value.warnings || [])]
  const numericFields = new Set((preview.value.standardFields || []).filter(field => field.numeric).map(field => field.code))
  Object.entries(columnMappings).forEach(([column, fieldCode]) => {
    if (!numericFields.has(fieldCode)) return
    const fieldLabel = preview.value.standardFields.find(field => field.code === fieldCode)?.label || fieldCode
    preview.value.previewRows.forEach((row, index) => {
      const value = row[column]
      if (value && !isDecimal(value) && warnings.length < 20) {
        warnings.push(`预览第${index + 1}行“${fieldLabel}”不是有效数字：${String(value).slice(0, 30)}`)
      }
    })
  })
  return [...new Set(warnings)]
})
const canUpload = computed(() => userStore.permissions.includes('*:*:*') || (
  userStore.permissions.includes('cost:boq:preview') && userStore.permissions.includes('cost:file:upload')
))
const canImport = computed(() => userStore.permissions.includes('*:*:*') || (
  userStore.permissions.includes('cost:boq:import') && userStore.permissions.includes('cost:file:query')
))

function handleFileChange(uploadFile) {
  const extension = uploadFile.name.includes('.') ? uploadFile.name.split('.').pop().toLowerCase() : ''
  if (!['xlsx', 'xls', 'csv'].includes(extension)) {
    proxy.$modal.msgError('仅支持 xlsx、xls、csv 文件')
    fileList.value = []
    selectedFile.value = null
    return
  }
  if (uploadFile.size / 1024 / 1024 > 10) {
    proxy.$modal.msgError('单个文件不能超过 10 MB')
    fileList.value = []
    selectedFile.value = null
    return
  }
  selectedFile.value = uploadFile.raw
  fileList.value = [uploadFile]
}

function handleFileRemove() {
  selectedFile.value = null
}

function handleExceed() {
  proxy.$modal.msgWarning('每次只能选择一个清单文件')
}

function startPreview() {
  if (!selectedFile.value) return
  loading.value = true
  uploadBoqPreview(props.projectId, selectedFile.value).then(response => {
    applyPreview(response.data)
    activeStep.value = 1
  }).finally(() => { loading.value = false })
}

function changeSheet(sheetName) {
  loading.value = true
  previewProjectBoqFile(preview.value.projectFileId, sheetName).then(response => {
    applyPreview(response.data)
  }).finally(() => { loading.value = false })
}

function applyPreview(data) {
  preview.value = data
  selectedSheet.value = data.selectedSheet
  Object.keys(columnMappings).forEach(key => delete columnMappings[key])
  Object.entries(data.mappingSuggestions || {}).forEach(([field, column]) => {
    columnMappings[column] = field
  })
  if (!importForm.batchName) {
    const baseName = (data.fileName || '工程量清单').replace(/\.(xlsx|xls|csv)$/i, '')
    importForm.batchName = `${baseName}-${data.selectedSheet}`.slice(0, 100)
  }
}

function isFieldUsed(fieldCode, currentColumn) {
  return Object.entries(columnMappings).some(([column, field]) => column !== currentColumn && field === fieldCode)
}

function confirmMapping() {
  const selectedFields = Object.values(columnMappings).filter(Boolean)
  if (!selectedFields.includes('itemName')) {
    proxy.$modal.msgError('必须映射“项目名称”字段')
    return
  }
  if (!['itemCode', 'quantity', 'unitPrice', 'totalPrice'].some(field => selectedFields.includes(field))) {
    proxy.$modal.msgError('项目编码、工程量、综合单价、合价至少需要映射一个')
    return
  }
  activeStep.value = 2
}

function mappedFieldLabel(columnKey) {
  const code = columnMappings[columnKey]
  return preview.value.standardFields.find(field => field.code === code)?.label || code
}

function isDecimal(raw) {
  let value = String(raw).trim().replace(/[,￥¥\s]/g, '')
  if (!value || value === '-') return true
  if (value.startsWith('(') && value.endsWith(')')) value = `-${value.slice(1, -1)}`
  return /^[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?$/.test(value)
}

function sheetLabel(sheet) {
  const suffix = sheet.detectedHeaderRow ? `表头第 ${sheet.detectedHeaderRow} 行，识别 ${sheet.recognizedFieldCount} 列` : '未识别表头'
  return `${sheet.name}（${suffix}）`
}

function executeImport() {
  if (!importForm.batchName.trim()) {
    proxy.$modal.msgError('请输入批次名称')
    return
  }
  if (!importForm.businessType) {
    proxy.$modal.msgError('请选择业务类型')
    return
  }
  importing.value = true
  const confirmedMappings = Object.fromEntries(
    Object.entries(columnMappings).filter(([, fieldCode]) => Boolean(fieldCode))
  )
  importBoq(props.projectId, {
    projectFileId: preview.value.projectFileId,
    batchName: importForm.batchName.trim(),
    businessType: importForm.businessType,
    sheetName: preview.value.selectedSheet,
    headerRow: preview.value.detectedHeaderRow,
    columnMappings: confirmedMappings,
    professionalType: importForm.professionalType || null
  }).then(response => {
    const result = response.data
    const message = result.failCount > 0
      ? `导入完成：成功 ${result.successCount} 条，失败 ${result.failCount} 条，请查看错误行`
      : `成功导入 ${result.successCount} 条清单`
    result.failCount > 0 ? proxy.$modal.msgWarning(message) : proxy.$modal.msgSuccess(message)
    emit('imported', result)
  }).finally(() => { importing.value = false })
}
</script>

<style scoped lang="scss">
.boq-preview { display: flex; flex-direction: column; gap: 16px; padding-top: 4px; }
.step-card { min-height: 420px; }
.step-heading, .mapping-toolbar { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; margin-bottom: 20px; }
.step-heading h3, .mapping-toolbar h3 { margin: 0 0 8px; font-size: 18px; }
.step-heading p, .mapping-toolbar p { margin: 0; color: var(--el-text-color-secondary); }
.step-actions { display: flex; justify-content: flex-end; margin-top: 22px; }
.step-actions.between { justify-content: space-between; }
.warning { margin-bottom: 10px; }
.import-form { margin-top: 22px; padding: 18px 16px 2px; background: var(--el-fill-color-lighter); border-radius: 6px; }
.mapped-header { display: flex; flex-direction: column; line-height: 1.35; }
.mapped-header small { color: var(--el-text-color-secondary); font-weight: 400; }
.boq-preview :deep(.el-upload), .boq-preview :deep(.el-upload-dragger) { width: 100%; }
</style>
