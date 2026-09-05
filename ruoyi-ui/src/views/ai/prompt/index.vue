<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch">
      <el-form-item label="编码" prop="promptCode"><el-input v-model="queryParams.promptCode" clearable @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="名称" prop="promptName"><el-input v-model="queryParams.promptName" clearable @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="状态" prop="enabled"><el-select v-model="queryParams.enabled" clearable style="width:120px"><el-option v-for="item in sys_normal_disable" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8"><el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['ai:prompt:add']">新增版本</el-button></el-col><right-toolbar v-model:showSearch="showSearch" @queryTable="getList" /></el-row>
    <el-table v-loading="loading" :data="rows">
      <el-table-column label="Prompt编码" prop="promptCode" min-width="170" />
      <el-table-column label="名称" prop="promptName" min-width="170" show-overflow-tooltip />
      <el-table-column label="版本" prop="version" width="90"><template #default="scope">v{{ scope.row.version }}</template></el-table-column>
      <el-table-column label="状态" prop="enabled" width="90"><template #default="scope"><dict-tag :options="sys_normal_disable" :value="scope.row.enabled" /></template></el-table-column>
      <el-table-column label="更新时间" prop="updateTime" width="170"><template #default="scope">{{ parseTime(scope.row.updateTime || scope.row.createTime) }}</template></el-table-column>
      <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" fixed="right" width="140"><template #default="scope"><el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['ai:prompt:edit']">修改</el-button><el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['ai:prompt:remove']">删除</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    <el-dialog :title="title" v-model="open" width="900px" append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="10"><el-form-item label="Prompt编码" prop="promptCode"><el-input v-model="form.promptCode" placeholder="例如 COST_QA" /></el-form-item></el-col>
          <el-col :span="10"><el-form-item label="名称" prop="promptName"><el-input v-model="form.promptName" /></el-form-item></el-col>
          <el-col :span="4"><el-form-item label="版本" prop="version"><el-input-number v-model="form.version" :min="1" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="System Prompt" prop="systemPrompt"><el-input v-model="form.systemPrompt" type="textarea" :rows="8" resize="vertical" /></el-form-item>
        <el-form-item label="用户模板" prop="userTemplate"><el-input v-model="form.userTemplate" type="textarea" :rows="8" resize="vertical" placeholder="可使用由业务服务负责替换的模板变量" /></el-form-item>
        <el-row :gutter="16"><el-col :span="8"><el-form-item label="状态"><el-radio-group v-model="form.enabled"><el-radio value="0">启用</el-radio><el-radio value="1">停用</el-radio></el-radio-group></el-form-item></el-col><el-col :span="16"><el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item></el-col></el-row>
      </el-form>
      <template #footer><el-button type="primary" @click="submitForm">确定</el-button><el-button @click="open=false">取消</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="AiPromptTemplate">
import { listPrompt, getPrompt, addPrompt, updatePrompt, deletePrompt } from '@/api/ai/prompt'
const { proxy } = getCurrentInstance()
const { sys_normal_disable } = useDict('sys_normal_disable')
const loading=ref(false),showSearch=ref(true),open=ref(false),rows=ref([]),total=ref(0),title=ref(''),form=ref({})
const queryParams=reactive({pageNum:1,pageSize:10,promptCode:undefined,promptName:undefined,enabled:undefined})
const rules={promptCode:[{required:true,message:'Prompt编码不能为空',trigger:'blur'},{pattern:/^[A-Za-z][A-Za-z0-9_.-]{0,63}$/,message:'编码格式不正确',trigger:'blur'}],promptName:[{required:true,message:'名称不能为空',trigger:'blur'}],systemPrompt:[{required:true,message:'System Prompt不能为空',trigger:'blur'}],userTemplate:[{required:true,message:'用户模板不能为空',trigger:'blur'}],version:[{required:true,message:'版本不能为空',trigger:'change'}]}
function reset(){form.value={id:undefined,promptCode:'',promptName:'',systemPrompt:'',userTemplate:'',version:1,enabled:'0',remark:''};proxy.resetForm('formRef')}
function getList(){loading.value=true;listPrompt(queryParams).then(r=>{rows.value=r.rows;total.value=r.total}).finally(()=>loading.value=false)}
function handleQuery(){queryParams.pageNum=1;getList()} function resetQuery(){proxy.resetForm('queryRef');handleQuery()}
function handleAdd(){reset();title.value='新增Prompt版本';open.value=true} function handleUpdate(row){reset();getPrompt(row.id).then(r=>{form.value=r.data;title.value='修改Prompt版本';open.value=true})}
function submitForm(){proxy.$refs.formRef.validate(valid=>{if(!valid)return;const action=form.value.id?updatePrompt:addPrompt;action(form.value).then(()=>{proxy.$modal.msgSuccess(form.value.id?'修改成功':'新增成功');open.value=false;getList()})})}
function handleDelete(row){proxy.$modal.confirm(`确认删除 ${row.promptCode} v${row.version} 吗？`).then(()=>deletePrompt(row.id)).then(()=>{proxy.$modal.msgSuccess('删除成功');getList()}).catch(()=>{})}
getList()
</script>
