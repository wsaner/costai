<template>
  <div class="app-container knowledge-page">
    <el-row :gutter="16">
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="full-card">
          <template #header><div class="card-head"><div><b>知识库</b><small>按用途隔离规范与企业资料</small></div><el-button v-hasPermi="['cost:knowledge:add']" type="primary" @click="openBase()">新建</el-button></div></template>
          <el-input v-model="keyword" clearable placeholder="搜索知识库" @keyup.enter="loadBases"><template #append><el-button icon="Search" @click="loadBases" /></template></el-input>
          <el-skeleton v-if="loadingBases" :rows="5" animated />
          <el-empty v-else-if="!bases.length" description="暂无知识库" />
          <div v-else class="base-list">
            <div v-for="base in bases" :key="base.id" class="base-item" :class="{active: base.id===activeId}" @click="selectBase(base)">
              <div class="base-title"><strong>{{ base.name }}</strong><dict-tag :options="knowledge_base_status" :value="base.status" /></div>
              <p>{{ base.description || '暂无说明' }}</p>
              <div class="metrics"><span>{{ base.documentCount }} 文档</span><span>{{ base.chunkCount }} 分片</span><span>TopK {{ base.topK }}</span></div>
              <div class="base-actions">
                <el-button v-hasPermi="['cost:knowledge:edit']" link type="primary" @click.stop="openBase(base)">设置</el-button>
                <el-button v-hasPermi="['cost:knowledge:remove']" link type="danger" @click.stop="removeBase(base)">删除</el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="full-card">
          <template #header><div class="card-head"><div><b>{{ activeBase?.name || '知识文档' }}</b><small>复用项目文件中心，不重复存储文件本体</small></div><div><el-button :disabled="!activeId" @click="loadDocuments">刷新</el-button><el-button v-hasPermi="['cost:knowledge:document:add']" type="primary" :disabled="!activeId" @click="openAttach">加入文档</el-button></div></div></template>
          <el-empty v-if="!activeId" description="请选择知识库" />
          <el-table v-else v-loading="loadingDocuments" :data="documents">
            <el-table-column prop="documentName" label="文档" min-width="210" show-overflow-tooltip />
            <el-table-column prop="documentType" label="格式" width="76" />
            <el-table-column label="状态" width="110"><template #default="scope"><dict-tag :options="knowledge_parse_status" :value="scope.row.parseStatus" /></template></el-table-column>
            <el-table-column prop="chunkCount" label="分片" width="72" align="right" />
            <el-table-column prop="indexedTime" label="索引时间" width="165" />
            <el-table-column label="操作" width="190" fixed="right"><template #default="scope">
              <el-button v-hasPermi="['cost:knowledge:query']" link type="primary" :disabled="scope.row.parseStatus!=='SUCCESS'" @click="openChunks(scope.row)">分片</el-button>
              <el-button v-hasPermi="['cost:knowledge:document:reindex']" link type="primary" :disabled="['WAITING','PARSING'].includes(scope.row.parseStatus)" @click="reindex(scope.row)">重建</el-button>
              <el-button v-hasPermi="['cost:knowledge:document:remove']" link type="danger" @click="removeDocument(scope.row)">删除</el-button>
            </template></el-table-column>
          </el-table>
          <el-alert v-for="doc in failedDocuments" :key="doc.id" class="doc-error" :title="`${doc.documentName}：${doc.errorMessage || statusHint(doc.parseStatus)}`" :type="doc.parseStatus==='OCR_REQUIRED'?'warning':'error'" :closable="false" show-icon />
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="activeId" shadow="never" class="rag-card">
      <template #header><div class="card-head"><div><b>知识问答</b><small>Embedding 检索 → 有限上下文 → LLM，回答附原文引用</small></div></div></template>
      <el-input v-model="question" type="textarea" :autosize="{minRows:2,maxRows:5}" maxlength="4000" show-word-limit placeholder="例如：结算审核中材料调差应重点核查哪些依据？" />
      <div class="ask-actions"><span>检索上下文受 TopK、相似度和字符上限控制</span><el-button v-hasPermi="['cost:knowledge:search']" type="primary" :loading="asking" :disabled="!question.trim()" @click="ask">检索并回答</el-button></div>
      <div v-if="answer" class="answer-grid">
        <div class="answer"><MarkdownViewer :content="answer" /></div>
        <aside><b>参考依据</b><el-empty v-if="!sources.length" description="无引用" :image-size="48" /><div v-for="(source,index) in sources" :key="source.chunkId" class="source"><el-tag size="small">来源{{ index+1 }}</el-tag><strong>《{{ source.documentName }}》</strong><span v-if="source.pageNumber">第{{ source.pageNumber }}页</span><p>{{ source.quote }}</p></div></aside>
      </div>
    </el-card>

    <el-dialog v-model="baseOpen" :title="baseForm.id?'修改知识库':'新建知识库'" width="560px">
      <el-form ref="baseFormRef" :model="baseForm" :rules="baseRules" label-width="112px">
        <el-form-item label="名称" prop="name"><el-input v-model="baseForm.name" maxlength="200" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="baseForm.description" type="textarea" maxlength="1000" show-word-limit /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="baseForm.status"><el-radio-button v-for="item in knowledge_base_status" :key="item.value" :value="item.value">{{ item.label }}</el-radio-button></el-radio-group></el-form-item>
        <el-form-item label="召回数量"><el-input-number v-model="baseForm.topK" :min="1" :max="20" /></el-form-item>
        <el-form-item label="相似度阈值"><el-input-number v-model="baseForm.similarityThreshold" :min="0" :max="1" :step="0.05" :precision="2" /></el-form-item>
        <el-form-item label="上下文字符上限"><el-input-number v-model="baseForm.maxContextChars" :min="1000" :max="50000" :step="1000" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="baseOpen=false">取消</el-button><el-button type="primary" :loading="savingBase" @click="saveBase">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="attachOpen" title="从项目文件加入知识库" width="560px">
      <el-alert title="文件本体仍由项目文件中心管理；本操作只建立知识库关联与向量索引。" type="info" :closable="false" show-icon />
      <el-form label-width="90px" class="attach-form">
        <el-form-item label="项目"><el-select v-model="attachForm.projectId" filterable placeholder="请选择有权访问的项目" @change="loadFileOptions"><el-option v-for="p in projects" :key="p.id" :label="`${p.projectCode} · ${p.projectName}`" :value="p.id" /></el-select></el-form-item>
        <el-form-item label="文件"><el-select v-model="attachForm.projectFileId" filterable placeholder="仅显示PDF、DOCX、TXT"><el-option v-for="file in files" :key="file.id" :label="file.originalName" :value="file.id"><span>{{ file.originalName }}</span><small class="file-meta">{{ file.fileExt?.toUpperCase() }}</small></el-option></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="attachOpen=false">取消</el-button><el-button type="primary" :loading="attaching" :disabled="!attachForm.projectFileId" @click="attach">加入并索引</el-button></template>
    </el-dialog>

    <el-drawer v-model="chunkOpen" :title="`${chunkDocument?.documentName || ''} · 分片`" size="68%">
      <el-table v-loading="loadingChunks" :data="chunks"><el-table-column prop="chunkIndex" label="#" width="60" /><el-table-column prop="pageNumber" label="页码" width="70" /><el-table-column prop="sectionTitle" label="章节" width="190" show-overflow-tooltip /><el-table-column prop="content" label="原文" min-width="360" show-overflow-tooltip /><el-table-column prop="charCount" label="字符" width="76" align="right" /></el-table>
      <pagination v-show="chunkTotal>0" :total="chunkTotal" v-model:page="chunkQuery.pageNum" v-model:limit="chunkQuery.pageSize" @pagination="loadChunks" />
    </el-drawer>
  </div>
</template>

<script setup name="CostKnowledge">
import { computed, getCurrentInstance, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownViewer from '@/components/MarkdownViewer/index.vue'
import { addKnowledgeBase, attachKnowledgeDocument, deleteKnowledgeBase, deleteKnowledgeDocument, listKnowledgeBases, listKnowledgeChunks, listKnowledgeDocuments, listKnowledgeFileOptions, listKnowledgeProjectOptions, queryKnowledge, reindexKnowledgeDocument, updateKnowledgeBase } from '@/api/cost/knowledge'

const { proxy } = getCurrentInstance()
const { knowledge_base_status, knowledge_parse_status } = proxy.useDict('knowledge_base_status', 'knowledge_parse_status')
const bases=ref([]),documents=ref([]),projects=ref([]),files=ref([]),chunks=ref([]),sources=ref([])
const activeId=ref(null),activeBase=ref(null),keyword=ref(''),question=ref(''),answer=ref('')
const loadingBases=ref(false),loadingDocuments=ref(false),asking=ref(false),baseOpen=ref(false),savingBase=ref(false),attachOpen=ref(false),attaching=ref(false),chunkOpen=ref(false),loadingChunks=ref(false),chunkDocument=ref(null),chunkTotal=ref(0)
const baseFormRef=ref(), baseForm=reactive({id:null,name:'',description:'',status:'ENABLED',topK:5,similarityThreshold:0.55,maxContextChars:12000})
const attachForm=reactive({projectId:null,projectFileId:null}), chunkQuery=reactive({pageNum:1,pageSize:20})
const baseRules={name:[{required:true,message:'请输入知识库名称',trigger:'blur'}]}
let polling
const failedDocuments=computed(()=>documents.value.filter(x=>['FAILED','OCR_REQUIRED'].includes(x.parseStatus)))

onMounted(loadBases)
onBeforeUnmount(()=>clearInterval(polling))
async function loadBases(){loadingBases.value=true;try{const r=await listKnowledgeBases({pageNum:1,pageSize:100,name:keyword.value});bases.value=r.rows||[];if(activeId.value){activeBase.value=bases.value.find(x=>x.id===activeId.value)||null}}finally{loadingBases.value=false}}
async function selectBase(base){activeId.value=base.id;activeBase.value=base;answer.value='';sources.value=[];await loadDocuments()}
async function loadDocuments(){if(!activeId.value)return;loadingDocuments.value=true;try{documents.value=(await listKnowledgeDocuments(activeId.value)).data||[];schedulePolling()}finally{loadingDocuments.value=false}}
function schedulePolling(){clearInterval(polling);if(documents.value.some(x=>['WAITING','PARSING'].includes(x.parseStatus)))polling=setInterval(loadDocuments,3000)}
function openBase(base){Object.assign(baseForm,base?{...base}:{id:null,name:'',description:'',status:'ENABLED',topK:5,similarityThreshold:0.55,maxContextChars:12000});baseOpen.value=true}
async function saveBase(){await baseFormRef.value.validate();savingBase.value=true;try{if(baseForm.id)await updateKnowledgeBase(baseForm);else await addKnowledgeBase(baseForm);baseOpen.value=false;await loadBases();ElMessage.success('保存成功')}finally{savingBase.value=false}}
async function removeBase(base){await ElMessageBox.confirm(`删除“${base.name}”将同时移除其文档关联、分片和向量索引，是否继续？`,'提示',{type:'warning'});await deleteKnowledgeBase(base.id);if(activeId.value===base.id){activeId.value=null;activeBase.value=null;documents.value=[]}await loadBases();ElMessage.success('删除成功')}
async function openAttach(){attachForm.projectId=null;attachForm.projectFileId=null;files.value=[];projects.value=(await listKnowledgeProjectOptions()).data||[];attachOpen.value=true}
async function loadFileOptions(){attachForm.projectFileId=null;files.value=attachForm.projectId?(await listKnowledgeFileOptions(attachForm.projectId)).data||[]:[]}
async function attach(){attaching.value=true;try{await attachKnowledgeDocument(activeId.value,attachForm.projectFileId);attachOpen.value=false;await loadDocuments();await loadBases();ElMessage.success('文档已加入，后台正在解析与索引')}finally{attaching.value=false}}
async function removeDocument(doc){await ElMessageBox.confirm(`确认移除“${doc.documentName}”及其向量索引吗？`,'提示',{type:'warning'});await deleteKnowledgeDocument(doc.id);await loadDocuments();await loadBases();ElMessage.success('删除成功')}
async function reindex(doc){await reindexKnowledgeDocument(doc.id);await loadDocuments();ElMessage.success('已提交重建索引')}
async function openChunks(doc){chunkDocument.value=doc;chunkQuery.pageNum=1;chunkOpen.value=true;await loadChunks()}
async function loadChunks(){loadingChunks.value=true;try{const r=await listKnowledgeChunks(chunkDocument.value.id,chunkQuery);chunks.value=r.rows||[];chunkTotal.value=r.total||0}finally{loadingChunks.value=false}}
async function ask(){asking.value=true;answer.value='';sources.value=[];try{const r=await queryKnowledge({knowledgeBaseId:activeId.value,question:question.value});answer.value=r.data.answer;sources.value=r.data.sources||[]}finally{asking.value=false}}
function statusHint(status){return status==='OCR_REQUIRED'?'扫描版PDF暂未引入OCR，请上传可检索文本版':'解析或索引失败，可查看提示后重建索引'}
</script>

<style scoped lang="scss">
.knowledge-page{background:var(--el-fill-color-extra-light);min-height:calc(100vh - 84px)}.full-card{min-height:520px}.card-head{display:flex;align-items:center;justify-content:space-between;gap:12px}.card-head>div:first-child{display:flex;flex-direction:column;gap:4px}.card-head small{font-weight:400;color:var(--el-text-color-secondary)}.base-list{margin-top:14px;max-height:445px;overflow:auto}.base-item{position:relative;padding:14px;margin-bottom:10px;border:1px solid var(--el-border-color-lighter);border-radius:10px;cursor:pointer}.base-item:hover,.base-item.active{border-color:var(--el-color-primary-light-5);background:var(--el-color-primary-light-9)}.base-title{display:flex;align-items:center;justify-content:space-between;padding-right:86px}.base-item p{height:36px;margin:8px 0;color:var(--el-text-color-secondary);font-size:13px;overflow:hidden}.metrics{display:flex;gap:16px;color:var(--el-text-color-secondary);font-size:12px}.base-actions{position:absolute;right:8px;top:8px}.doc-error{margin-top:10px}.rag-card{margin-top:16px}.ask-actions{display:flex;justify-content:space-between;align-items:center;margin-top:10px;color:var(--el-text-color-secondary);font-size:12px}.answer-grid{display:grid;grid-template-columns:minmax(0,1fr) 330px;gap:18px;margin-top:18px}.answer{padding:18px;border-radius:10px;background:var(--el-fill-color-extra-light)}.answer-grid aside{padding:16px;border-left:1px solid var(--el-border-color-lighter)}.source{margin-top:12px;padding:10px;border-radius:8px;background:var(--el-fill-color-light)}.source strong,.source span{margin-left:6px;font-size:13px}.source p{margin:8px 0 0;color:var(--el-text-color-secondary);font-size:12px;line-height:1.6}.attach-form{margin-top:18px}.attach-form .el-select{width:100%}.file-meta{float:right;color:var(--el-text-color-secondary)}@media(max-width:1000px){.answer-grid{grid-template-columns:1fr}.answer-grid aside{border-left:0;border-top:1px solid var(--el-border-color-lighter)}}
</style>
