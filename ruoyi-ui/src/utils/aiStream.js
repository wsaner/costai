import { getToken } from '@/utils/auth'

export async function streamAiChat(conversationId, content, onEvent, signal) {
  const response = await fetch(`${import.meta.env.VITE_APP_BASE_API}/ai/chat/conversations/${conversationId}/messages/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=utf-8',
      Authorization: `Bearer ${getToken()}`
    },
    body: JSON.stringify({ content }),
    signal
  })
  if (!response.ok) {
    let message = `AI服务请求失败（${response.status}）`
    try { message = (await response.json()).msg || message } catch (_) { /* 非JSON错误响应 */ }
    throw new Error(message)
  }
  if (!response.body) throw new Error('当前浏览器不支持流式响应')
  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  const dispatch = block => {
    let event = 'message'
    const data = []
    block.split(/\r?\n/).forEach(line => {
      if (line.startsWith('event:')) event = line.slice(6).trim()
      if (line.startsWith('data:')) data.push(line.slice(5).trimStart())
    })
    if (!data.length) return
    const raw = data.join('\n')
    let payload = raw
    try { payload = JSON.parse(raw) } catch (_) { /* 文本事件 */ }
    onEvent(event, payload)
  }
  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
    const blocks = buffer.split(/\r?\n\r?\n/)
    buffer = blocks.pop() || ''
    blocks.forEach(dispatch)
    if (done) break
  }
  if (buffer.trim()) dispatch(buffer)
}
