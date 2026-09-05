<template>
  <div class="app-container">
    <el-alert title="审计日志只记录调用元数据、Token、耗时和脱敏错误，不保存API Key、Prompt或响应正文。" type="info" show-icon :closable="false" class="mb8" />
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch">
      <el-form-item label="请求类型" prop="requestType"><el-select v-model="queryParams.requestType" clearable style="width:180px"><el-option v-for="item in requestTypes" :key="item" :label="item" :value="item" /></el-select></el-form-item>
      <el-form-item label="业务类型" prop="businessType"><el-input v-model="queryParams.businessType" clearable /></el-form-item>
      <el-form-item label="结果" prop="success"><el-select v-model="queryParams.success" clearable style="width:110px"><el-option label="成功" value="Y" /><el-option label="失败" value="N" /></el-select></el-form-item>
      <el-form-item label="调用时间"><el-date-picker v-model="dateRange" value-format="YYYY-MM-DD" type="daterange" range-separator="-" start-placeholder="开始" end-placeholder="结束" /></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row class="mb8"><right-toolbar v-model:showSearch="showSearch" @queryTable="getList" /></el-row>
    <el-table v-loading="loading" :data="rows">
      <el-table-column label="时间" prop="createTime" width="170"><template #default="scope">{{ parseTime(scope.row.createTime) }}</template></el-table-column>
      <el-table-column label="调用人" prop="createBy" width="110" />
      <el-table-column label="配置" prop="modelConfigName" min-width="130" show-overflow-tooltip />
      <el-table-column label="模型" prop="modelName" min-width="130" show-overflow-tooltip />
      <el-table-column label="请求类型" prop="requestType" width="160" />
      <el-table-column label="业务" min-width="140"><template #default="scope">{{ [scope.row.businessType, scope.row.businessId].filter(Boolean).join(' / ') || '-' }}</template></el-table-column>
      <el-table-column label="Token" prop="totalTokens" width="100" align="right" />
      <el-table-column label="耗时" prop="durationMs" width="105" align="right"><template #default="scope">{{ scope.row.durationMs }} ms</template></el-table-column>
      <el-table-column label="结果" prop="success" width="80"><template #default="scope"><el-tag :type="scope.row.success==='Y'?'success':'danger'">{{ scope.row.success==='Y'?'成功':'失败' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" fixed="right" width="80"><template #default="scope"><el-button link type="primary" @click="showDetail(scope.row)" v-hasPermi="['ai:log:query']">详情</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    <el-drawer v-model="drawer" title="AI调用详情" size="520px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="请求ID">{{ detail.requestId || '-' }}</el-descriptions-item><el-descriptions-item label="提供商">{{ detail.providerType || '-' }}</el-descriptions-item><el-descriptions-item label="模型">{{ detail.modelName || '-' }}</el-descriptions-item><el-descriptions-item label="Prompt Token">{{ detail.promptTokens }}</el-descriptions-item><el-descriptions-item label="Completion Token">{{ detail.completionTokens }}</el-descriptions-item><el-descriptions-item label="总Token">{{ detail.totalTokens }}</el-descriptions-item><el-descriptions-item label="耗时">{{ detail.durationMs }} ms</el-descriptions-item><el-descriptions-item label="错误码">{{ detail.errorCode || '-' }}</el-descriptions-item><el-descriptions-item label="错误原因">{{ detail.errorMessage || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup name="AiRequestLog">
import { listAiRequestLog, getAiRequestLog } from '@/api/ai/log'
const {proxy}=getCurrentInstance(), loading=ref(false),showSearch=ref(true),rows=ref([]),total=ref(0),dateRange=ref([]),drawer=ref(false),detail=ref(null)
const requestTypes=['CHAT','STREAM_CHAT','STRUCTURED_CHAT','EMBEDDING','CONNECTION_TEST']
const queryParams=reactive({pageNum:1,pageSize:10,requestType:undefined,businessType:undefined,success:undefined})
function getList(){loading.value=true;listAiRequestLog(proxy.addDateRange(queryParams,dateRange.value)).then(r=>{rows.value=r.rows;total.value=r.total}).finally(()=>loading.value=false)}
function handleQuery(){queryParams.pageNum=1;getList()} function resetQuery(){dateRange.value=[];proxy.resetForm('queryRef');handleQuery()}
function showDetail(row){getAiRequestLog(row.id).then(r=>{detail.value=r.data;drawer.value=true})}
getList()
</script>
