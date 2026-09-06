<template>
  <div class="chat-page">
    <aside class="conversation-panel">
      <div class="panel-head"><strong>历史会话</strong><el-button v-hasPermi="['ai:chat:use']" type="primary" link @click="openCreate">新建</el-button></div>
      <el-skeleton v-if="loadingConversations" :rows="5" animated />
      <el-empty v-else-if="!conversations.length" description="暂无会话" :image-size="72" />
      <div v-else class="conversation-list">
        <div v-for="item in conversations" :key="item.id" class="conversation-item" :class="{ active: item.id === activeId }" @click="selectConversation(item)">
          <div class="conversation-title">{{ item.title }}</div>
          <div class="conversation-meta"><span>{{ item.mode === 'PROJECT' ? item.projectName || '项目问答' : '通用问答' }}</span><span>{{ item.messageCount || 0 }} 条</span></div>
          <el-button v-hasPermi="['ai:chat:remove']" class="delete-btn" link type="danger" @click.stop="removeConversation(item)">删除</el-button>
        </div>
      </div>
    </aside>

    <main class="chat-main">
      <div class="chat-head">
        <div><h3>{{ activeConversation?.title || 'AI造价助手' }}</h3><span>{{ modeLabel(activeConversation?.mode) }}</span></div>
        <el-tag v-if="activeConversation?.projectName" type="success" effect="plain">{{ activeConversation.projectName }}</el-tag>
      </div>
      <div ref="messageBox" class="message-list">
        <el-empty v-if="!activeConversation" description="新建或选择一个会话开始咨询" />
        <div v-else-if="!messages.length" class="welcome">
          <div class="welcome-icon">AI</div><h2>有什么造价问题需要分析？</h2>
          <p>可询问清单、审核风险、项目指标或造价规范。项目模式只读取经权限校验的有限摘要。</p>
        </div>
        <div v-for="item in messages" :key="item.id" class="message-row" :class="item.role.toLowerCase()">
          <div class="avatar">{{ item.role === 'USER' ? '我' : 'AI' }}</div>
          <div class="message-bubble">
            <MarkdownViewer v-if="item.role === 'ASSISTANT'" :content="item.content" />
            <div v-else class="user-content">{{ item.content }}</div>
            <div v-if="item.status === 'STREAMING'" class="typing"><i /><i /><i /></div>
            <el-alert v-if="item.status === 'FAILED'" :title="item.errorMessage || '回答生成失败'" type="error" :closable="false" show-icon />
          </div>
        </div>
      </div>
      <div class="composer">
        <el-input v-model="input" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }" maxlength="4000" show-word-limit
                  :disabled="!activeConversation || streaming" placeholder="输入造价问题，Enter发送，Shift+Enter换行" @keydown="handleKeydown" />
        <div class="composer-actions"><span>AI建议仅供专业人员复核</span><el-button v-hasPermi="['ai:chat:use']" type="primary" :loading="streaming" :disabled="!input.trim() || !activeConversation" @click="send">发送</el-button></div>
      </div>
    </main>

    <aside class="context-panel">
      <div class="panel-head"><strong>项目上下文</strong></div>
      <template v-if="activeConversation">
        <el-form label-position="top">
          <el-form-item label="问答模式"><el-select v-model="contextForm.mode" :disabled="streaming" @change="modeChanged"><el-option v-for="dict in ai_conversation_mode" :key="dict.value" :label="dict.label" :value="dict.value" /></el-select></el-form-item>
          <el-form-item v-if="contextForm.mode === 'PROJECT'" label="关联项目">
            <el-select v-model="contextForm.projectId" filterable :disabled="streaming" placeholder="选择有权访问的项目" @visible-change="loadProjects" @change="saveContext">
              <el-option v-for="p in projects" :key="p.id" :label="`${p.projectCode} · ${p.projectName}`" :value="p.id" />
            </el-select>
          </el-form-item>
        </el-form>
        <el-divider content-position="left">AI工具</el-divider>
        <el-empty v-if="!toolCalls.length" description="发送项目问题后显示" :image-size="56" />
        <div v-for="tool in toolCalls" :key="tool.name" class="tool-card"><el-tag size="small">{{ tool.name }}</el-tag><span>{{ tool.scope }}</span><b>{{ tool.resultCount }} 条</b></div>
        <el-divider content-position="left">引用资料</el-divider>
        <el-empty v-if="!sources.length" description="暂无引用" :image-size="56" />
        <div v-for="(source, index) in sources" :key="index" class="source-card"><el-tag size="small" type="info">{{ source.type }}</el-tag><span>{{ source.title }}</span></div>
      </template>
      <el-empty v-else description="请选择会话" :image-size="60" />
    </aside>

    <el-dialog v-model="createOpen" title="新建会话" width="460px">
      <el-form label-position="top">
        <el-form-item label="问答模式"><el-radio-group v-model="createForm.mode"><el-radio-button value="GENERAL">通用造价问答</el-radio-button><el-radio-button value="PROJECT">基于项目问答</el-radio-button></el-radio-group></el-form-item>
        <el-form-item v-if="createForm.mode === 'PROJECT'" label="关联项目" required><el-select v-model="createForm.projectId" filterable placeholder="请选择项目" @visible-change="loadProjects"><el-option v-for="p in projects" :key="p.id" :label="`${p.projectCode} · ${p.projectName}`" :value="p.id" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="createOpen=false">取消</el-button><el-button type="primary" :loading="creating" @click="createNew">创建</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="AiCostAssistant">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownViewer from '@/components/MarkdownViewer/index.vue'
import { createConversation, deleteConversation, listChatProjectOptions, listConversations, listMessages, updateConversation } from '@/api/ai/chat'
import { streamAiChat } from '@/utils/aiStream'

const { proxy } = getCurrentInstance()
const { ai_conversation_mode } = proxy.useDict('ai_conversation_mode')
const conversations = ref([]), messages = ref([]), projects = ref([])
const activeId = ref(null), activeConversation = ref(null), input = ref(''), messageBox = ref(null)
const loadingConversations = ref(false), streaming = ref(false), createOpen = ref(false), creating = ref(false)
const sources = ref([]), toolCalls = ref([])
const createForm = reactive({ mode: 'GENERAL', projectId: null })
const contextForm = reactive({ mode: 'GENERAL', projectId: null })
let abortController

onMounted(loadConversations)
onBeforeUnmount(() => abortController?.abort())

async function loadConversations() {
  loadingConversations.value = true
  try { conversations.value = (await listConversations({ pageNum: 1, pageSize: 100 })).rows || [] }
  finally { loadingConversations.value = false }
}
async function selectConversation(item) {
  if (streaming.value) return ElMessage.warning('请等待当前回答完成')
  activeId.value = item.id; activeConversation.value = item
  contextForm.mode = item.mode; contextForm.projectId = item.projectId
  sources.value = []; toolCalls.value = []
  const response = await listMessages(item.id, { pageNum: 1, pageSize: 200 })
  messages.value = response.rows || []
  const lastAssistant = [...messages.value].reverse().find(m => m.role === 'ASSISTANT')
  if (lastAssistant) { sources.value = parseJson(lastAssistant.sourcesJson); toolCalls.value = parseJson(lastAssistant.toolCallsJson) }
  scrollBottom()
}
function openCreate() { createForm.mode = 'GENERAL'; createForm.projectId = null; createOpen.value = true }
async function createNew() {
  if (createForm.mode === 'PROJECT' && !createForm.projectId) return ElMessage.warning('请选择关联项目')
  creating.value = true
  try {
    const id = (await createConversation(createForm)).data; createOpen.value = false
    await loadConversations(); const item = conversations.value.find(x => x.id === id); if (item) await selectConversation(item)
  } finally { creating.value = false }
}
async function removeConversation(item) {
  await ElMessageBox.confirm(`确认删除会话“${item.title}”吗？`, '提示', { type: 'warning' })
  await deleteConversation(item.id)
  if (activeId.value === item.id) { activeId.value = null; activeConversation.value = null; messages.value = [] }
  await loadConversations(); ElMessage.success('删除成功')
}
async function modeChanged(mode) {
  if (mode === 'GENERAL') { contextForm.projectId = null; await saveContext() }
  else loadProjects(true)
}
async function saveContext() {
  if (contextForm.mode === 'PROJECT' && !contextForm.projectId) return
  await updateConversation({ id: activeId.value, title: activeConversation.value.title, ...contextForm })
  await loadConversations(); activeConversation.value = conversations.value.find(x => x.id === activeId.value)
  ElMessage.success('上下文已更新')
}
async function loadProjects(visible) {
  if (!visible || projects.value.length) return
  const response = await listChatProjectOptions({ pageNum: 1, pageSize: 100 })
  projects.value = response.rows || []
}
function handleKeydown(event) { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); send() } }
async function send() {
  const content = input.value.trim(); if (!content || streaming.value || !activeId.value) return
  const userTemp = { id: `u-${Date.now()}`, role: 'USER', content, status: 'COMPLETED' }
  const assistantTemp = { id: `a-${Date.now()}`, role: 'ASSISTANT', content: '', status: 'STREAMING' }
  messages.value.push(userTemp, assistantTemp); input.value = ''; streaming.value = true; sources.value = []; toolCalls.value = []
  scrollBottom(); abortController = new AbortController()
  try {
    await streamAiChat(activeId.value, content, (event, data) => {
      if (event === 'meta') { userTemp.id = data.userMessageId; assistantTemp.id = data.assistantMessageId }
      if (event === 'delta') { assistantTemp.content += data.text || ''; scrollBottom() }
      if (event === 'context') { sources.value = data.sources || []; toolCalls.value = data.toolCalls || [] }
      if (event === 'done') assistantTemp.status = 'COMPLETED'
      if (event === 'error') { assistantTemp.status = 'FAILED'; assistantTemp.errorMessage = data.message }
    }, abortController.signal)
  } catch (error) {
    if (error.name !== 'AbortError') { assistantTemp.status = 'FAILED'; assistantTemp.errorMessage = error.message; ElMessage.error(error.message) }
  } finally {
    streaming.value = false; abortController = null; await loadConversations()
  }
}
function scrollBottom() { nextTick(() => { if (messageBox.value) messageBox.value.scrollTop = messageBox.value.scrollHeight }) }
function parseJson(value) { try { return value ? JSON.parse(value) : [] } catch (_) { return [] } }
function modeLabel(mode) { return mode === 'PROJECT' ? '基于项目问答' : '通用造价问答' }
</script>

<style scoped lang="scss">
.chat-page { height: calc(100vh - 84px); min-height: 620px; display: grid; grid-template-columns: 250px minmax(480px,1fr) 290px; background: var(--el-bg-color); border: 1px solid var(--el-border-color-lighter); }
.conversation-panel,.context-panel { min-width: 0; padding: 16px; background: var(--el-fill-color-extra-light); overflow: auto; }
.conversation-panel { border-right: 1px solid var(--el-border-color-lighter); }.context-panel{border-left:1px solid var(--el-border-color-lighter)}
.panel-head { display:flex;align-items:center;justify-content:space-between;margin-bottom:16px }
.conversation-item { position:relative;padding:12px;margin-bottom:8px;border-radius:10px;cursor:pointer;background:var(--el-bg-color);border:1px solid transparent }
.conversation-item:hover,.conversation-item.active{border-color:var(--el-color-primary-light-5);background:var(--el-color-primary-light-9)}
.conversation-title{padding-right:34px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-weight:600}.conversation-meta{display:flex;justify-content:space-between;margin-top:7px;font-size:12px;color:var(--el-text-color-secondary)}
.delete-btn{position:absolute;right:6px;top:6px;opacity:0}.conversation-item:hover .delete-btn{opacity:1}
.chat-main{min-width:0;display:flex;flex-direction:column}.chat-head{height:64px;flex:none;padding:0 22px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid var(--el-border-color-lighter)}
.chat-head h3{margin:0 0 3px}.chat-head span{font-size:12px;color:var(--el-text-color-secondary)}
.message-list{flex:1;overflow:auto;padding:24px max(24px,8%)}.welcome{text-align:center;margin:14vh auto 0;max-width:580px;color:var(--el-text-color-secondary)}.welcome h2{color:var(--el-text-color-primary)}
.welcome-icon{width:54px;height:54px;margin:auto;border-radius:16px;display:grid;place-items:center;color:#fff;font-weight:700;background:linear-gradient(135deg,var(--el-color-primary),#6c63ff)}
.message-row{display:flex;gap:12px;margin-bottom:22px}.message-row.user{flex-direction:row-reverse}.avatar{width:34px;height:34px;flex:none;border-radius:10px;display:grid;place-items:center;background:var(--el-color-primary);color:#fff;font-size:13px}.message-row.assistant .avatar{background:#605bff}
.message-bubble{max-width:82%;padding:12px 15px;border-radius:14px;background:var(--el-fill-color-light)}.user .message-bubble{background:var(--el-color-primary);color:#fff}.user-content{white-space:pre-wrap;line-height:1.65}
.composer{flex:none;padding:14px max(24px,8%) 18px;border-top:1px solid var(--el-border-color-lighter)}.composer-actions{display:flex;justify-content:space-between;align-items:center;margin-top:8px;font-size:12px;color:var(--el-text-color-secondary)}
.typing{display:flex;gap:4px;padding-top:8px}.typing i{width:6px;height:6px;border-radius:50%;background:var(--el-color-primary);animation:pulse 1.2s infinite}.typing i:nth-child(2){animation-delay:.15s}.typing i:nth-child(3){animation-delay:.3s}@keyframes pulse{50%{opacity:.25;transform:translateY(-2px)}}
.tool-card,.source-card{display:grid;grid-template-columns:auto 1fr auto;gap:8px;align-items:start;padding:10px;margin-bottom:8px;border-radius:8px;background:var(--el-bg-color);font-size:12px}.source-card{grid-template-columns:auto 1fr}.tool-card span,.source-card span{overflow-wrap:anywhere;color:var(--el-text-color-secondary)}
@media(max-width:1100px){.chat-page{grid-template-columns:220px 1fr}.context-panel{display:none}}@media(max-width:760px){.chat-page{grid-template-columns:1fr}.conversation-panel{display:none}.message-list,.composer{padding-left:14px;padding-right:14px}}
</style>
