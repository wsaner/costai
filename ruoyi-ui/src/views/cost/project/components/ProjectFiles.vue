<template>
  <div class="project-files">
    <el-card v-hasPermi="['cost:file:upload']" shadow="never" class="upload-card">
      <div class="upload-toolbar">
        <div>
          <div class="section-title">上传项目资料</div>
          <div class="section-tip">单文件不超过 10 MB；DWG、DXF、IFC 当前仅保存，不进行解析。</div>
        </div>
        <el-select v-model="uploadCategory" placeholder="请选择文件分类" style="width: 220px">
          <el-option v-for="item in cost_file_category" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </div>
      <el-upload
        ref="uploadRef"
        v-model:file-list="uploadList"
        drag
        multiple
        :action="uploadUrl"
        :headers="uploadHeaders"
        :data="{ fileCategory: uploadCategory }"
        :accept="acceptedExtensions"
        :limit="20"
        :before-upload="beforeUpload"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :on-exceed="handleExceed"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到这里，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 xlsx、xls、csv、pdf、doc、docx、txt、png、jpg、jpeg、zip、dwg、dxf、ifc</div>
        </template>
      </el-upload>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="table-header">
          <span class="section-title">项目文件</span>
          <el-button icon="Refresh" :loading="loading" @click="loadFiles">刷新</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="files" empty-text="暂无项目文件">
        <el-table-column label="文件名" min-width="240" show-overflow-tooltip>
          <template #default="scope">
            <div class="file-name-cell">
              <el-icon><Document /></el-icon>
              <span>{{ scope.row.originalName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="文件分类" width="190">
          <template #default="scope">
            <el-select
              v-if="canEditCategory"
              v-model="scope.row.fileCategory"
              size="small"
              @change="value => handleCategoryChange(scope.row, value)"
            >
              <el-option v-for="item in cost_file_category" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <dict-tag v-else :options="cost_file_category" :value="scope.row.fileCategory" />
          </template>
        </el-table-column>
        <el-table-column prop="fileExt" label="格式" width="80" />
        <el-table-column label="大小" width="110">
          <template #default="scope">{{ formatFileSize(scope.row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="createBy" label="上传人" width="110" show-overflow-tooltip />
        <el-table-column label="上传时间" width="170">
          <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="AI解析状态" width="120">
          <template #default="scope">
            <el-tooltip :disabled="!scope.row.aiParseError" :content="scope.row.aiParseError" placement="top">
              <dict-tag :options="cost_ai_parse_status" :value="scope.row.aiParseStatus" />
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="scope">
            <el-button v-hasPermi="['cost:file:download']" link type="primary" icon="Download" @click="handleDownload(scope.row)">下载</el-button>
            <el-tooltip content="AI解析任务将在后续阶段接入" placement="top">
              <span><el-button link type="primary" icon="RefreshRight" disabled>重新解析</el-button></span>
            </el-tooltip>
            <el-button v-hasPermi="['cost:file:remove']" link type="danger" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup name="ProjectFiles">
import { getToken } from '@/utils/auth'
import useUserStore from '@/store/modules/user'
import { listProjectFiles, updateProjectFileCategory, deleteProjectFile } from '@/api/cost/file'

const props = defineProps({
  projectId: { type: [Number, String], required: true }
})
const { proxy } = getCurrentInstance()
const userStore = useUserStore()
const { cost_file_category, cost_ai_parse_status } = useDict('cost_file_category', 'cost_ai_parse_status')
const loading = ref(false)
const files = ref([])
const uploadList = ref([])
const uploadCategory = ref('')
const uploadRef = ref()
const acceptedExtensions = '.xlsx,.xls,.csv,.pdf,.doc,.docx,.txt,.png,.jpg,.jpeg,.zip,.dwg,.dxf,.ifc'
const uploadUrl = computed(() => `${import.meta.env.VITE_APP_BASE_API}/cost/project/${props.projectId}/files`)
const uploadHeaders = computed(() => ({ Authorization: `Bearer ${getToken()}` }))
const canEditCategory = computed(() => userStore.permissions.includes('*:*:*') || userStore.permissions.includes('cost:file:edit'))

watch(cost_file_category, options => {
  if (!uploadCategory.value && options && options.length) {
    uploadCategory.value = options[0].value
  }
}, { immediate: true })

function loadFiles() {
  loading.value = true
  listProjectFiles(props.projectId).then(response => {
    files.value = response.data || []
  }).finally(() => { loading.value = false })
}

function beforeUpload(file) {
  if (!uploadCategory.value) {
    proxy.$modal.msgError('请先选择文件分类')
    return false
  }
  const extension = file.name.includes('.') ? file.name.split('.').pop().toLowerCase() : ''
  const accepted = acceptedExtensions.split(',').map(item => item.substring(1))
  if (!accepted.includes(extension)) {
    proxy.$modal.msgError('不支持该文件格式')
    return false
  }
  if (file.size / 1024 / 1024 > 10) {
    proxy.$modal.msgError('单个文件不能超过 10 MB')
    return false
  }
  return true
}

function handleUploadSuccess(response) {
  if (response.code !== 200) {
    proxy.$modal.msgError(response.msg || '上传失败')
    return
  }
  proxy.$modal.msgSuccess('文件上传成功')
  uploadList.value = []
  loadFiles()
}

function handleUploadError(error) {
  let message = '文件上传失败'
  try {
    const response = JSON.parse(error.message)
    message = response.msg || message
  } catch (_) {
    // 使用统一友好提示，避免向用户展示底层错误。
  }
  proxy.$modal.msgError(message)
}

function handleExceed() {
  proxy.$modal.msgWarning('一次最多上传 20 个文件')
}

function handleCategoryChange(row, value) {
  updateProjectFileCategory(row.id, value).then(() => {
    proxy.$modal.msgSuccess('文件分类已更新')
  }).catch(() => loadFiles())
}

function handleDownload(row) {
  proxy.$download.projectFile(row.id, row.originalName)
}

function handleDelete(row) {
  proxy.$modal.confirm(`确认删除文件“${row.originalName}”吗？`).then(() => {
    return deleteProjectFile(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    loadFiles()
  }).catch(() => {})
}

function formatFileSize(bytes) {
  const value = Number(bytes || 0)
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}

loadFiles()
</script>

<style scoped lang="scss">
.project-files { display: flex; flex-direction: column; gap: 16px; padding-top: 4px; }
.upload-card { border-style: dashed; }
.upload-toolbar, .table-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.upload-toolbar { margin-bottom: 16px; }
.section-title { color: var(--el-text-color-primary); font-size: 16px; font-weight: 600; }
.section-tip { margin-top: 5px; color: var(--el-text-color-secondary); font-size: 13px; }
.file-name-cell { display: flex; align-items: center; gap: 8px; }
.project-files :deep(.el-upload), .project-files :deep(.el-upload-dragger) { width: 100%; }
</style>
