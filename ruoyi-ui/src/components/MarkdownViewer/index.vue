<template><div class="markdown-body" v-html="safeHtml" /></template>

<script setup>
import { computed } from 'vue'
import DOMPurify from 'dompurify'
import { marked } from 'marked'

const props = defineProps({ content: { type: String, default: '' } })
marked.setOptions({ gfm: true, breaks: true })
const safeHtml = computed(() => DOMPurify.sanitize(marked.parse(props.content || '')))
</script>

<style scoped lang="scss">
.markdown-body { line-height: 1.72; color: var(--el-text-color-primary); overflow-wrap: anywhere; }
.markdown-body :deep(p) { margin: 0 0 10px; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }
.markdown-body :deep(pre) { overflow: auto; padding: 12px; border-radius: 8px; background: #172033; color: #e6edf3; }
.markdown-body :deep(code) { font-family: ui-monospace, SFMono-Regular, Consolas, monospace; }
.markdown-body :deep(:not(pre) > code) { padding: 2px 5px; border-radius: 4px; background: var(--el-fill-color-light); }
.markdown-body :deep(blockquote) { margin: 10px 0; padding: 2px 12px; border-left: 4px solid var(--el-color-primary); color: var(--el-text-color-secondary); }
.markdown-body :deep(table) { width: 100%; margin: 10px 0; border-collapse: collapse; }
.markdown-body :deep(th), .markdown-body :deep(td) { padding: 7px 10px; border: 1px solid var(--el-border-color); text-align: left; }
.markdown-body :deep(th) { background: var(--el-fill-color-light); }
.markdown-body :deep(a) { color: var(--el-color-primary); }
</style>
