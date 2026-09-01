# CostAI 第一阶段 API 设计

> 状态：阶段 01 项目管理接口已实现；后续模块仍为契约草案。  
> 普通响应沿用若依 `AjaxResult` / `TableDataInfo`；AI 流式响应使用 `text/event-stream`。

## 1. 通用约定

- Base URL 沿用前端 `VITE_APP_BASE_API`。
- 鉴权：`Authorization: Bearer <token>`。
- 列表：`pageNum/pageSize/orderByColumn/isAsc`，响应 `code/msg/rows/total`。
- 普通成功：`{ "code": 200, "msg": "...", "data": ... }`。
- 业务失败使用现有 `ServiceException` → `AjaxResult.error`，不返回堆栈。
- 金额 JSON 建议以 number 输出并由 Java `BigDecimal` 生成；前端不得用浮点重新计算审计金额。
- 时间沿用 `yyyy-MM-dd HH:mm:ss`。
- 批量删除沿用若依 `DELETE /resource/{ids}` 风格，但项目等高风险资源必须先做依赖校验和数据权限校验。
- API 文档使用 springdoc；需把 CostAI Controller 加入扫描组，不能维持当前只扫描 `com.ruoyi.web.controller.tool` 的配置。

## 2. 项目管理 `/cost/project`（阶段 01 已实现）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/cost/project/list` | `cost:project:list` | 分页列表，支持名称、编号、类型、阶段、状态、负责人、创建时间范围 |
| GET | `/cost/project/{id}` | `cost:project:query` | 详情，校验项目数据权限 |
| POST | `/cost/project` | `cost:project:add` | 新增项目，服务端校验编号并计算核减率 |
| PUT | `/cost/project` | `cost:project:edit` | 编辑项目，校验数据权限、编号和负责人范围 |
| DELETE | `/cost/project/{ids}` | `cost:project:remove` | 批量逻辑删除，逐项校验数据权限 |
| PUT | `/cost/project/changeStatus` | `cost:project:edit` | 修改项目状态，状态值来自字典 |
| GET | `/cost/project/statistics` | `cost:project:list` | 当前筛选与数据权限范围内的项目概览 |
| GET | `/cost/project/managerOptions` | `cost:project:list` | 当前用户可见的负责人选项，最多 100 条 |

项目列表、详情与统计使用 `@DataScope(deptAlias="p", userAlias="p", deptField="owner_dept_id", userField="project_manager_id")`。详情访问由独立 Spring Bean 承载，以确保修改、删除等内部权限校验也经过 AOP，而不是被同类调用绕开。

统计响应：

```json
{
  "code": 200,
  "data": {
    "projectCount": 0,
    "submittedAmount": 0,
    "approvedAmount": 0,
    "reductionAmount": 0,
    "averageReductionRate": 0
  }
}
```

`averageReductionRate` 是汇总核减额除以汇总送审额的加权口径，不是各项目百分比的算术平均。金额和比率均由 Java `BigDecimal` 产生。

SpringDoc 已新增 `cost` 分组（显示名 `CostAI造价平台`），扫描 `/cost/**` 与 `com.ruoyi.web.controller.cost`；项目 Controller 的 8 个接口均包含中文摘要。

## 3. 项目文件 `/cost/projects/{projectId}/files`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/cost/projects/{projectId}/files` | `cost:file:list` | 文件列表 |
| POST | `/cost/projects/{projectId}/files` | `cost:file:upload` | multipart 上传并登记元数据 |
| GET | `/cost/files/{fileId}` | `cost:file:query` | 元数据与解析状态 |
| GET | `/cost/files/{fileId}/download` | `cost:file:download` | 授权下载，不暴露任意本地路径 |
| DELETE | `/cost/files/{fileId}` | `cost:file:remove` | 逻辑删除并安排物理清理 |
| POST | `/cost/files/{fileId}/parse` | `cost:file:parse` | 创建解析任务，返回 taskId |
| GET | `/cost/files/{fileId}/parsed-text` | `cost:file:query` | 分页/分段读取解析文本 |

上传响应必须包含 `fileId/originalName/fileSize/parseStatus`，不能只返回路径。CSV、ZIP 等使用 CostAI 专用安全校验。

## 4. BOQ 导入与清单 `/cost/boq`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/cost/boq/imports/preview` | `cost:boq:import` | 上传或选择 projectFile，读取表头/样本，创建批次 |
| GET | `/cost/boq/imports/{batchId}/mapping` | `cost:boq:import` | 获取建议映射、置信度和样本 |
| PUT | `/cost/boq/imports/{batchId}/mapping` | `cost:boq:import` | 用户确认字段映射 |
| POST | `/cost/boq/imports/{batchId}/execute` | `cost:boq:import` | 创建正式导入任务，返回 taskId |
| GET | `/cost/boq/imports/{batchId}` | `cost:boq:query` | 批次状态和统计 |
| GET | `/cost/boq/imports/{batchId}/errors` | `cost:boq:query` | 导入错误明细 |
| GET | `/cost/boq/items` | `cost:boq:list` | 按项目、批次、角色分页查询清单 |
| GET | `/cost/boq/items/{id}` | `cost:boq:query` | 清单详情与 Excel 定位 |
| PUT | `/cost/boq/items` | `cost:boq:edit` | 人工修正，记录操作日志 |
| POST | `/cost/boq/items/export` | `cost:boq:export` | 清单导出 |

字段映射响应示例：

```json
{
  "code": 200,
  "data": {
    "batchId": 101,
    "headerRowNo": 3,
    "sheets": ["分部分项"],
    "mappings": [
      {
        "sourceColumn": "清单编号",
        "targetField": "itemCode",
        "confidence": 0.98,
        "source": "ALIAS_RULE",
        "required": false
      }
    ],
    "sampleRows": []
  }
}
```

## 5. BOQ 对比 `/cost/boq/compares`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/cost/boq/compares` | `cost:review:start` | 选择 source/target 批次创建对比 |
| GET | `/cost/boq/compares/{id}` | `cost:review:view` | 对比统计 |
| GET | `/cost/boq/compares/{id}/items` | `cost:review:view` | 匹配/漏项/新增/歧义列表 |
| PUT | `/cost/boq/compares/{id}/matches/{resultId}` | `cost:review:match` | 人工调整匹配 |
| POST | `/cost/boq/compares/{id}/rerun` | `cost:review:start` | 以新批次重跑，不覆盖历史 |

## 6. 审核 `/cost/reviews`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/cost/reviews` | `cost:review:start` | 创建规则+AI 审核任务 |
| GET | `/cost/reviews/{id}` | `cost:review:view` | 审核批次摘要 |
| GET | `/cost/reviews/{id}/items` | `cost:review:view` | 审核清单列表 |
| GET | `/cost/reviews/{id}/issues` | `cost:review:view` | 问题分页列表 |
| GET | `/cost/review-issues/{issueId}` | `cost:review:view` | Drawer 详情、证据与引用 |
| PUT | `/cost/review-issues/{issueId}/status` | `cost:review:handle` | 确认/忽略/整改，需意见 |
| POST | `/cost/reviews/{id}/export` | `cost:review:export` | 导出问题明细 |

启动审核请求只返回 `reviewBatchId/taskId`，不等待 LLM 完成。确定性规则可先写问题；AI enrichment 在事务外进行并逐批更新。

## 7. AI Provider `/ai/providers`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/ai/providers` | `ai:provider:list` | Provider 列表，只返回 keyHint |
| GET | `/ai/providers/{id}` | `ai:provider:query` | 详情，不返回密钥/密文 |
| POST | `/ai/providers` | `ai:provider:add` | 新增并加密 Key |
| PUT | `/ai/providers` | `ai:provider:edit` | 空 Key 表示保持原值 |
| DELETE | `/ai/providers/{ids}` | `ai:provider:remove` | 有任务/Prompt 引用时限制删除 |
| PUT | `/ai/providers/{id}/status` | `ai:provider:edit` | 启停并清缓存 |
| POST | `/ai/providers/{id}/test` | `ai:provider:test` | 受限连通性测试，记录 AI 日志 |

## 8. Prompt `/ai/prompts`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/ai/prompts` | `ai:prompt:list` | 模板列表 |
| POST | `/ai/prompts` | `ai:prompt:add` | 创建模板与首个草稿版本 |
| GET | `/ai/prompts/{id}/versions` | `ai:prompt:query` | 版本列表 |
| POST | `/ai/prompts/{id}/versions` | `ai:prompt:edit` | 新建草稿版本 |
| PUT | `/ai/prompts/{id}/versions/{versionId}` | `ai:prompt:edit` | 编辑草稿，不改已发布版 |
| POST | `/ai/prompts/{id}/versions/{versionId}/publish` | `ai:prompt:publish` | 发布并原子切换 activeVersion |
| PUT | `/ai/prompts/{id}/status` | `ai:prompt:edit` | 启停 |

## 9. AI 聊天 `/ai/chat`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/ai/chat/sessions` | `ai:chat:list` | 当前用户历史会话 |
| POST | `/ai/chat/sessions` | `ai:chat:use` | 创建会话，可关联项目/知识库 |
| GET | `/ai/chat/sessions/{id}/messages` | `ai:chat:view` | 消息与来源分页 |
| PUT | `/ai/chat/sessions/{id}` | `ai:chat:use` | 改标题/当前项目/知识库 |
| DELETE | `/ai/chat/sessions/{id}` | `ai:chat:remove` | 仅会话所有者或授权管理员 |
| POST | `/ai/chat/sessions/{id}/messages` | `ai:chat:use` | `text/event-stream` 流式回答 |
| POST | `/ai/chat/messages/{id}/stop` | `ai:chat:use` | 请求停止生成 |

流式请求使用 fetch POST，可设置 Authorization。建议事件：

```text
event: meta       data: {messageId, requestId}
event: delta      data: {text}
event: source     data: {sourceType, name, pageNumber, chunkId, score}
event: tool_call  data: {name, status, displayArgs}
event: usage      data: {promptTokens, completionTokens, totalTokens}
event: done       data: {messageId, suggestions}
event: error      data: {code, message, retryable}
```

所有 `data` 是单行 JSON。前端断线后通过消息列表恢复已落库内容；MVP 不承诺从字符位置无缝续传。

## 10. 知识库 `/cost/knowledge-bases`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/cost/knowledge-bases` | `cost:knowledge:list` | 知识库列表 |
| POST | `/cost/knowledge-bases` | `cost:knowledge:add` | 新建知识库 |
| PUT | `/cost/knowledge-bases` | `cost:knowledge:edit` | 配置 TopK/阈值/上下文预算 |
| DELETE | `/cost/knowledge-bases/{ids}` | `cost:knowledge:remove` | 检查文档后异步清理向量 |
| GET | `/cost/knowledge-bases/{id}/documents` | `cost:knowledge:list` | 文档列表 |
| POST | `/cost/knowledge-bases/{id}/documents` | `cost:knowledge:add` | 上传/选择项目文件并创建索引任务 |
| GET | `/cost/knowledge-documents/{id}` | `cost:knowledge:query` | 解析和索引状态 |
| POST | `/cost/knowledge-documents/{id}/reindex` | `cost:knowledge:edit` | 新任务重建索引 |
| DELETE | `/cost/knowledge-documents/{id}` | `cost:knowledge:remove` | 删除文档与向量 |
| GET | `/cost/knowledge-documents/{id}/chunks` | `cost:knowledge:query` | 分片分页/原文定位 |
| POST | `/cost/knowledge-bases/{id}/search` | `cost:knowledge:search` | 调试检索，仅授权管理员/知识维护者 |

## 11. AI 任务 `/ai/tasks`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/ai/tasks` | `ai:task:list` | 当前权限范围任务列表 |
| GET | `/ai/tasks/{id}` | `ai:task:query` | 任务进度/结果摘要/错误 |
| POST | `/ai/tasks/{id}/cancel` | `ai:task:cancel` | 设置 cancelRequested，由 Worker 安全停止 |
| POST | `/ai/tasks/{id}/retry` | `ai:task:retry` | 失败任务按幂等边界重试 |
| GET | `/ai/tasks/stream` | `ai:task:list` | SSE 推送当前用户任务变化 |

任务 SSE 可使用 `GET` 时，原生 EventSource 无法带 Authorization；仍建议复用 fetch 流封装，或未来增加一次性短期 stream ticket。禁止把长期 JWT 放查询参数。

## 12. AI 日志 `/ai/logs`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/ai/logs` | `ai:log:list` | 请求日志分页 |
| GET | `/ai/logs/{id}` | `ai:log:query` | 脱敏详情 |
| GET | `/ai/logs/statistics` | `ai:log:list` | Token、成功率、时延聚合 |
| POST | `/ai/logs/export` | `ai:log:export` | 脱敏导出 |

## 13. 报告 `/cost/reports`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/cost/reports` | `cost:report:generate` | 选择项目/审核批次/模板创建任务 |
| GET | `/cost/reports` | `cost:report:list` | 报告列表 |
| GET | `/cost/reports/{id}` | `cost:report:query` | 报告结构化预览与状态 |
| PUT | `/cost/reports/{id}/content` | `cost:report:edit` | 人工修改 AI 建议文字 |
| POST | `/cost/reports/{id}/regenerate` | `cost:report:generate` | 生成新版本，不覆盖历史 |
| GET | `/cost/reports/{id}/download` | `cost:report:download` | 下载 Word/PDF |

## 14. 工作台 `/cost/dashboard`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/cost/dashboard/summary` | `cost:dashboard:view` | 项目与 AI 指标 |
| GET | `/cost/dashboard/amount-trend` | `cost:dashboard:view` | 最近 12 月送审/审定/核减趋势 |
| GET | `/cost/dashboard/issue-statistics` | `cost:dashboard:view` | 问题类型与风险分布 |
| GET | `/cost/dashboard/recent-projects` | `cost:dashboard:view` | 最近项目，应用数据权限 |

所有统计都必须从真实业务表聚合，并应用项目数据权限；禁止前端 Mock。

## 15. 受控 AI Tool

Tool 不直接暴露为公共 REST API，而是注册在 `AiToolRegistry`，每次执行接收 `AgentContext` 并通过 Service 校验权限：

| Tool | 依赖 Service | 权限重点 |
|---|---|---|
| `searchKnowledge` | KnowledgeQueryService | 知识库可见范围 |
| `searchProject` | CostProjectService | 项目数据权限 |
| `searchBoq` | BoqQueryService | 项目 + `cost:boq:query` |
| `searchReviewIssues` | CostReviewIssueService | 项目 + `cost:review:view` |
| `calculateCostDifference` | CostCalculator | 纯函数，无外部写入 |
| `generateReport` | CostReportService | `cost:report:generate`，需用户显式动作 |

第一阶段 Tool 均使用白名单参数 DTO、分页/数量上限和超时。禁止任意 SQL、任意 URL 请求、文件系统路径和 shell 命令。
