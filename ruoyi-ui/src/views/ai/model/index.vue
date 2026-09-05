<template>
  <div class="app-container">
    <el-alert title="API Key 仅在保存时传输，数据库中使用 AES-256-GCM 加密，页面只显示脱敏摘要。" type="info" show-icon :closable="false" class="mb8" />
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch">
      <el-form-item label="配置名称" prop="name"><el-input v-model="queryParams.name" clearable @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="提供商" prop="providerType">
        <el-select v-model="queryParams.providerType" clearable style="width: 190px">
          <el-option v-for="item in ai_provider_type" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="enabled">
        <el-select v-model="queryParams.enabled" clearable style="width: 120px">
          <el-option v-for="item in sys_normal_disable" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['ai:model:add']">新增</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="rows">
      <el-table-column label="配置名称" prop="name" min-width="140" show-overflow-tooltip />
      <el-table-column label="提供商" prop="providerType" width="170"><template #default="scope"><dict-tag :options="ai_provider_type" :value="scope.row.providerType" /></template></el-table-column>
      <el-table-column label="基础地址" prop="baseUrl" min-width="220" show-overflow-tooltip />
      <el-table-column label="对话模型" prop="chatModel" min-width="140" show-overflow-tooltip />
      <el-table-column label="Embedding模型" prop="embeddingModel" min-width="150" show-overflow-tooltip />
      <el-table-column label="密钥" prop="apiKeyHint" width="120"><template #default="scope">{{ scope.row.apiKeyHint || '未配置' }}</template></el-table-column>
      <el-table-column label="默认" prop="isDefault" width="75"><template #default="scope"><el-tag :type="scope.row.isDefault === 'Y' ? 'success' : 'info'">{{ scope.row.isDefault === 'Y' ? '是' : '否' }}</el-tag></template></el-table-column>
      <el-table-column label="状态" prop="enabled" width="80"><template #default="scope"><dict-tag :options="sys_normal_disable" :value="scope.row.enabled" /></template></el-table-column>
      <el-table-column label="操作" fixed="right" width="210">
        <template #default="scope">
          <el-button link type="primary" icon="Connection" :loading="testingId === scope.row.id" @click="handleTest(scope.row)" v-hasPermi="['ai:model:test']">测试</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['ai:model:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['ai:model:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="720px" append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="配置名称" prop="name"><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="提供商" prop="providerType"><el-select v-model="form.providerType" style="width:100%"><el-option v-for="item in ai_provider_type" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
        </el-row>
        <el-form-item label="基础地址" prop="baseUrl"><el-input v-model="form.baseUrl" placeholder="例如 https://api.example.com/v1" /></el-form-item>
        <el-form-item label="API Key"><el-input v-model="form.apiKey" type="password" show-password autocomplete="new-password" :placeholder="form.id ? '留空表示保持原密钥' : '本地无鉴权网关可留空'" /></el-form-item>
        <el-form-item v-if="form.id && form.apiKeyHint" label="当前密钥"><span>{{ form.apiKeyHint }}</span><el-checkbox v-model="form.clearApiKey" class="ml10">清除密钥</el-checkbox></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="对话模型" prop="chatModel"><el-input v-model="form.chatModel" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Embedding模型"><el-input v-model="form.embeddingModel" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="温度"><el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="最大Token"><el-input-number v-model="form.maxTokens" :min="1" :max="200000" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="超时(秒)"><el-input-number v-model="form.timeoutSeconds" :min="1" :max="600" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="状态"><el-radio-group v-model="form.enabled"><el-radio value="0">启用</el-radio><el-radio value="1">停用</el-radio></el-radio-group></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="默认模型"><el-switch v-model="form.isDefault" active-value="Y" inactive-value="N" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="submitForm">确定</el-button><el-button @click="open=false">取消</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="AiModelConfig">
import { listModelConfig, getModelConfig, addModelConfig, updateModelConfig, deleteModelConfig, testModelConfig } from '@/api/ai/model'

const { proxy } = getCurrentInstance()
const { ai_provider_type, sys_normal_disable } = useDict('ai_provider_type', 'sys_normal_disable')
const loading = ref(false), showSearch = ref(true), open = ref(false)
const rows = ref([]), total = ref(0), title = ref(''), testingId = ref(null)
const queryParams = reactive({ pageNum: 1, pageSize: 10, name: undefined, providerType: undefined, enabled: undefined })
const form = ref({})
const rules = {
  name: [{ required: true, message: '配置名称不能为空', trigger: 'blur' }],
  providerType: [{ required: true, message: '提供商不能为空', trigger: 'change' }],
  baseUrl: [{ required: true, message: '基础地址不能为空', trigger: 'blur' }],
  chatModel: [{ required: true, message: '对话模型不能为空', trigger: 'blur' }]
}
function reset() { form.value = { id: undefined, name: '', providerType: 'OPENAI_COMPATIBLE', baseUrl: '', apiKey: '', clearApiKey: false, chatModel: '', embeddingModel: '', temperature: 0.2, maxTokens: 4096, timeoutSeconds: 60, enabled: '0', isDefault: 'N', remark: '' }; proxy.resetForm('formRef') }
function getList() { loading.value = true; listModelConfig(queryParams).then(r => { rows.value = r.rows; total.value = r.total }).finally(() => loading.value = false) }
function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleAdd() { reset(); title.value = '新增AI模型配置'; open.value = true }
function handleUpdate(row) { reset(); getModelConfig(row.id).then(r => { form.value = { ...r.data, apiKey: '', clearApiKey: false }; title.value = '修改AI模型配置'; open.value = true }) }
function submitForm() { proxy.$refs.formRef.validate(valid => { if (!valid) return; const action = form.value.id ? updateModelConfig : addModelConfig; action(form.value).then(() => { proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功'); open.value = false; getList() }) }) }
function handleDelete(row) { proxy.$modal.confirm(`确认删除模型配置“${row.name}”吗？`).then(() => deleteModelConfig(row.id)).then(() => { proxy.$modal.msgSuccess('删除成功'); getList() }).catch(() => {}) }
function handleTest(row) { testingId.value = row.id; testModelConfig(row.id).then(r => proxy.$modal.msgSuccess(`连接成功，模型：${r.data.model || row.chatModel}`)).finally(() => testingId.value = null) }
getList()
</script>
