# CostAI 第一阶段 API 设计

> 状态：阶段 01 至阶段 10 已实现；当前包含项目、文件、BOQ预览/导入/匹配、规则与AI审核、统一LLM基础设施和AI造价助手SSE聊天。
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
- API 文档使用 springdoc；已复用现有配置增加 `cost` 分组，扫描 `/cost/**` 与 CostAI Controller。

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

## 3. 项目文件（阶段 02 已实现）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/cost/project/{projectId}/files/list` | `cost:file:list` | 项目文件列表 |
| POST | `/cost/project/{projectId}/files` | `cost:file:upload` | multipart 的 `file` + `fileCategory`，上传并登记元数据 |
| GET | `/cost/project/files/{id}` | `cost:file:query` | 文件元数据；不返回存储路径和解析正文 |
| PUT | `/cost/project/files/{id}/category` | `cost:file:edit` | 修改字典分类，请求体 `{ "fileCategory": "BOQ" }` |
| DELETE | `/cost/project/files/{id}` | `cost:file:remove` | 逻辑删除，事务提交后删除物理文件 |
| GET | `/cost/project/files/{id}/download` | `cost:file:download` | 鉴权下载；不暴露存储路径 |
| GET | `/cost/project/files/{id}/parse-status` | `cost:file:query` | 查询 `WAITING/PARSING/SUCCESS/FAILED/UNSUPPORTED` 状态 |

所有接口都根据文件记录中的真实 `project_id` 重新查询项目并应用现有数据权限，不能通过篡改 URL 中的项目 ID 读取其他项目。上传响应包含业务 `fileId` 和元数据，但不含 `storagePath/aiParseText`。重新解析操作仅在页面预留，待持久化 AI 任务中心接入后再增加接口，当前不返回假任务。

阶段 01 的项目删除接口现已增加文件依赖保护：存在未删除的项目文件时返回“项目存在文件，请先删除项目文件”，避免遗留不可访问的物理资料。

## 4. BOQ Excel/CSV 预览与字段识别（阶段 03 已实现）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/cost/boq/preview/upload/{projectId}` | `cost:boq:preview` 且 `cost:file:upload` | multipart 参数 `file`；先写入现有项目文件中心（分类 `BOQ`），再流式识别并返回预览 |
| POST | `/cost/boq/preview/files/{projectFileId}` | `cost:boq:preview` 且 `cost:file:query` | 对已存项目文件重新预览或切换 Sheet；请求体可选 `{ "sheetName": "分部分项" }` |

两条接口都会从真实文件记录反查 `project_id`，再经过阶段 01 的项目数据权限校验。预览专用上传只接受 xlsx/xls/csv；其他项目资料仍走阶段 02 通用文件接口。预览接口本身不创建导入批次；用户确认映射后调用阶段 04 正式导入接口。

预览响应核心结构：

```json
{
  "code": 200,
  "data": {
    "projectFileId": 1,
    "fileName": "工程量清单.xlsx",
    "sheets": [{ "index": 0, "name": "分部分项", "detectedHeaderRow": 3, "sampledRowCount": 100, "recognizedFieldCount": 8 }],
    "selectedSheet": "分部分项",
    "detectedHeaderRow": 3,
    "columns": [{ "index": 0, "key": "A", "header": "清单编号", "sampleValues": ["010101"] }],
    "previewRows": [{ "A": "010101", "B": "挖一般土方" }],
    "mappingSuggestions": { "itemCode": "A", "itemName": "B" },
    "standardFields": [{ "code": "itemCode", "label": "项目编码", "numeric": false }],
    "warnings": []
  }
}
```

标准字段为 `sequenceNo/itemCode/itemName/itemFeature/unit/quantity/unitPrice/totalPrice/laborPrice/materialPrice/machinePrice/managementFee/profit/tax`。`detectedHeaderRow` 为用户可读的 1 基行号；`previewRows` 最多 50 条。

### 4.1 正式导入与清单（阶段 04 已实现）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/cost/boq/imports/{projectId}` | `cost:boq:import` 且 `cost:file:query` | 根据项目文件、Sheet 和已确认映射同步流式导入，返回批次统计 |
| GET | `/cost/boq/batches` | `cost:boq:list` | 按 `projectId/batchName/businessType` 分页查询批次 |
| GET | `/cost/boq/batches/{batchId}` | `cost:boq:query` | 批次详情和汇总 |
| GET | `/cost/boq/items` | `cost:boq:list` | 按 `batchId/itemCode/itemName/professionalType` 分页查询清单 |
| GET | `/cost/boq/batches/{batchId}/errors` | `cost:boq:query` | 分页查询错误行及 Excel 定位 |
| DELETE | `/cost/boq/batches/{batchId}` | `cost:boq:remove` | 同一事务逻辑删除错误行、清单和批次 |

正式导入请求示例：

```json
{
  "projectFileId": 10,
  "batchName": "送审预算-分部分项",
  "businessType": "SUBMITTED",
  "sheetName": "分部分项",
  "headerRow": 3,
  "columnMappings": {
    "A": "sequenceNo",
    "B": "itemCode",
    "C": "itemName",
    "D": "quantity",
    "E": "unitPrice",
    "F": "totalPrice"
  },
  "professionalType": "CIVIL"
}
```

响应中的 `totalCount/successCount/failCount/totalAmount/importStatus` 来自实际持久化结果。业务类型为 `BOQ/CONTROL_PRICE/BID_PRICE/SUBMITTED/REVIEWED/SETTLEMENT/OTHER`，状态为 `IMPORTING/SUCCESS/PARTIAL_FAILED/FAILED`。文件按行流式读取、每 500 条短事务写入；`totalPrice` 保留 Excel 原值，`calculatedTotalPrice` 单独返回供后续审核。

所有批次、明细和错误查询先根据数据库中的项目/批次关系应用项目数据权限，URL/查询参数不能改写真实归属。批次分页查询的权限校验会隔离并恢复 PageHelper 上下文，避免校验 SQL 消耗业务分页。

阶段 04 暂不提供清单人工编辑、导出、异步取消；这些能力需结合变更审计和 AI 任务中心在后续任务单独实现，不返回假任务。

## 5. BOQ 对比 `/cost/boq/compares`（阶段 05 已实现）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/cost/boq/compares` | `cost:compare:start` | 首次匹配两个批次；已有结果时拒绝覆盖 |
| POST | `/cost/boq/compares/rematch` | `cost:compare:start` | 删除自动结果并重新匹配，保留人工结果 |
| GET | `/cost/boq/compares` | `cost:compare:list` | 按项目/左右批次分页，可筛选状态和编码/名称 |
| GET | `/cost/boq/compares/summary` | `cost:compare:list` | 返回各匹配状态数量与平均匹配度 |
| GET | `/cost/boq/compares/batch-options/{projectId}` | `cost:compare:list` | 当前项目可对比的导入批次 |
| GET | `/cost/boq/compares/item-options` | `cost:compare:manual` | 按项目/批次分页搜索人工候选清单 |
| PUT | `/cost/boq/compares/manual` | `cost:compare:manual` | 指定一条左清单与一条右清单人工匹配 |
| PUT | `/cost/boq/compares/{compareId}/unmatch` | `cost:compare:manual` | 取消已匹配记录并恢复左右未匹配行 |

首次/重新匹配请求为 `{projectId,leftBatchId,rightBatchId}`；人工请求另含 `leftItemId/rightItemId`。所有入口均从数据库中的批次/清单关系反查项目并复用项目数据权限，不能通过篡改 `projectId` 跨项目访问。匹配结果的一条清单最多参与一个当前匹配；差异口径统一为左值减右值，差异率为差异除以左值绝对值，左值为 0 时返回 `null`。

当前阶段按用户指定的 `cost_boq_compare` 表维护现行结果，不提供虚假的历史版本。重新匹配保留 `MANUAL`，自动结果可重复生成；需要审核证据版本化时，由后续审核批次保存算法/规则版本和结果快照。

## 6. 审核任务与问题管理 `/cost/review`（阶段 06/07/09 已实现）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/cost/review/tasks` | `cost:review:start` | 对两个已匹配批次同步执行纯 Java 规则审核并创建任务 |
| GET | `/cost/review/tasks` | `cost:review:list` | 按项目分页查询审核任务 |
| GET | `/cost/review/tasks/{reviewTaskId}` | `cost:review:query` | 查询任务、规则版本及问题分类/风险/核减统计 |
| GET | `/cost/review/tasks/{reviewTaskId}/issues` | `cost:review:list` | 问题分页；支持问题类型、级别、状态和关键词 |
| GET | `/cost/review/issues/{issueId}` | `cost:review:query` | 查询问题详情、左右清单 ID、规则证据及人工处理记录 |
| PUT | `/cost/review/issues/{issueId}` | `cost:review:handle` | 确认、忽略、整改，或保存风险等级和审核意见 |
| POST | `/cost/review/issues/{issueId}/ai-analysis` | `cost:review:ai` | 对一个语义候选做有限上下文 Structured Output 分析 |
| GET | `/cost/review/rule-configs` | `cost:review:config` | 查询数据库化规则配置，按规则/顺序返回 |
| PUT | `/cost/review/rule-configs/{configId}` | `cost:review:config` | 修改配置值并校验类型和阈值关系 |

启动请求为 `{projectId,leftBatchId,rightBatchId,taskName?}`。Service 会从数据库反查两个批次并应用现有项目数据权限，拒绝相同批次、跨项目批次以及尚未生成对比结果的批次组合；任务名为空时按左右批次名称生成。成功时返回完整任务摘要；阶段 06/07 不创建 AI 任务、不调用 LLM。

问题差异统一为左值减右值。比例分母优先取左值绝对值，左值为 0 时回退到右值绝对值；两侧都为 0 时为 0。规则执行前读取 20 项数据库配置并写入审核批次快照，之后修改配置只影响新审核。问题以 500 条一批写入短事务，风险金额为每个规则问题的确定性暴露金额绝对值汇总。

人工处理请求为 `{status,issueLevel,reviewComment}`，其中审核意见必填、最长 2000 字。允许的主流程为待确认→确认/忽略，已确认→整改；忽略和整改可按受控矩阵退回待确认或确认。每次处理同事务写入处理人、时间并刷新任务统计。任务 `reductionAmount` 只汇总状态为 `CONFIRMED/RECTIFIED`、问题类型为 `TOTAL_PRICE` 且差异为正的金额，不把全部风险暴露冒充核减金额。

AI分析请求为 `{modelConfigId?,additionalContext?}`，补充上下文最长 2000 字。Service 先通过审核任务反查项目并应用现有数据权限，再只读取当前问题关联的左、右各一条清单；确定性负数、零单价、合价计算错误等不会进入AI。总业务上下文上限 12,000 字符，输出上限 900 Token。响应 `data.analysis` 严格包含 `{hasIssue,issueType,riskLevel,title,analysis,suggestion,confidence}`，另返回模型、requestId 和 tokenUsage；`data.issue` 是落库后的建议快照。模型调用不持有事务，短事务仅写 `ai_*` 字段，不修改清单、原问题类型/级别或人工状态。操作日志不保存请求/响应正文，AI请求日志只保存Token、耗时与脱敏错误。

## 7. AI 模型配置 `/ai/model-configs`（阶段 08 已实现）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/ai/model-configs/list` | `ai:model:list` | 分页列表，只返回 `apiKeyHint` |
| GET | `/ai/model-configs/{id}` | `ai:model:query` | 详情，不序列化 API Key 或密文 |
| POST | `/ai/model-configs` | `ai:model:add` | 新增配置；API Key 在 Service 加密 |
| PUT | `/ai/model-configs` | `ai:model:edit` | 修改；空 Key 保持原值，`clearApiKey=true` 明确清除 |
| DELETE | `/ai/model-configs/{ids}` | `ai:model:remove` | 逻辑删除模型配置 |
| POST | `/ai/model-configs/{id}/test` | `ai:model:test` | 连接测试，允许停用配置并记录 AI 日志 |

模型保存体为 `{id?,name,providerType,baseUrl,apiKey?,clearApiKey?,chatModel,embeddingModel?,temperature,maxTokens,timeoutSeconds,enabled,isDefault,remark?}`。`baseUrl` 只允许无账号、查询参数和片段的 HTTP(S) 地址；为兼容本地网关允许 HTTP。服务端不跟随重定向，防止 Authorization 跨站泄露。

Java 业务层统一依赖 `AiModelService`：`chat`、`streamChat`、`structuredChat`、`embedding`。流式方法以增量回调暴露，后续聊天模块可桥接现有 MVC SSE；Structured Output 使用 `response_format.type=json_schema` 并校验响应为 JSON。所有网络调用均在数据库事务外执行。

## 8. Prompt `/ai/prompts`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/ai/prompts/list` | `ai:prompt:list` | 按编码、名称、状态分页；列表不返回长正文 |
| GET | `/ai/prompts/{id}` | `ai:prompt:query` | 查询一个 Prompt 版本完整内容 |
| POST | `/ai/prompts` | `ai:prompt:add` | 新增 Prompt 版本 |
| PUT | `/ai/prompts` | `ai:prompt:edit` | 修改 Prompt 版本；启用时停用同编码其他版本 |
| DELETE | `/ai/prompts/{ids}` | `ai:prompt:remove` | 逻辑删除 Prompt 版本 |

## 9. AI 造价助手 `/ai/chat/conversations`（阶段 10 已实现）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/ai/chat/conversations/list` | `ai:chat:list` | 当前用户历史会话分页 |
| GET | `/ai/chat/conversations/project-options` | `ai:chat:list` | 当前项目数据权限范围内的轻量选项 |
| GET | `/ai/chat/conversations/{id}` | `ai:chat:list` | 当前用户会话详情 |
| POST | `/ai/chat/conversations` | `ai:chat:use` | 创建 `GENERAL/PROJECT` 会话 |
| PUT | `/ai/chat/conversations` | `ai:chat:use` | 修改标题、模式或关联项目 |
| DELETE | `/ai/chat/conversations/{id}` | `ai:chat:remove` | 仅逻辑删除当前用户会话及消息 |
| GET | `/ai/chat/conversations/{id}/messages` | `ai:chat:list` | 当前用户消息分页 |
| POST | `/ai/chat/conversations/{id}/messages/stream` | `ai:chat:use` | 带JWT的POST SSE流式回答 |

流式请求使用 fetch POST，可设置 Authorization。建议事件：

```text
event: meta       data: {conversationId,userMessageId,assistantMessageId}
event: context    data: {data,toolCalls,sources}
event: delta      data: {text}
event: usage      data: {promptTokens, completionTokens, totalTokens}
event: done       data: {messageId,status}
event: error      data: {message}
```

前端使用 `fetch + ReadableStream`，因此可继续携带现有 Bearer JWT；不把Token放入URL。项目模式在发起网络调用前通过 `CostProjectService` 校验数据权限，再调用 `getProjectSummary`、`getReviewIssues` 和按明确关键词触发的 `searchBoq`。最近历史最多20条/30000字符，审核问题和清单搜索各最多10条。前端断线后可通过消息列表恢复已落库状态；阶段10不承诺字符级续传或服务端主动取消。

## 10. 知识库 `/cost/knowledge`（阶段 11 已实现）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/cost/knowledge/list` | `cost:knowledge:list` | 知识库分页列表 |
| GET | `/cost/knowledge/{id}` | `cost:knowledge:query` | 知识库详情 |
| POST | `/cost/knowledge` | `cost:knowledge:add` | 新建知识库 |
| PUT | `/cost/knowledge` | `cost:knowledge:edit` | 配置状态、TopK、阈值和上下文字符上限 |
| DELETE | `/cost/knowledge/{id}` | `cost:knowledge:remove` | 逻辑删除关联数据，提交后清理Qdrant集合，不删除项目原文件 |
| GET | `/cost/knowledge/{baseId}/documents` | `cost:knowledge:list` | 文档及解析/索引状态列表 |
| POST | `/cost/knowledge/{baseId}/documents` | `cost:knowledge:document:add` | 将有权访问的项目文件加入知识库并后台索引 |
| DELETE | `/cost/knowledge/documents/{id}` | `cost:knowledge:document:remove` | 删除文档关联、分片和文档向量 |
| POST | `/cost/knowledge/documents/{id}/reindex` | `cost:knowledge:document:reindex` | 失败/OCR/成功文档重新解析索引 |
| GET | `/cost/knowledge/documents/{id}/chunks` | `cost:knowledge:query` | 分片分页和原文定位 |
| GET | `/cost/knowledge/project-options` | `cost:knowledge:document:add` | 按现有项目数据权限列出项目 |
| GET | `/cost/knowledge/project/{projectId}/file-options` | `cost:knowledge:document:add` | 列出可访问的PDF/DOCX/TXT项目文件 |
| POST | `/cost/knowledge/query` | `cost:knowledge:search` | Embedding检索、有限上下文问答并返回来源 |

`POST /query` 请求为 `{knowledgeBaseId,question,topK?}`，返回 `{answer,sources,tokenUsage}`；每个来源包含 `chunkId/documentName/pageNumber/sectionTitle/score/quote`。Qdrant只提供候选ID和分值，服务端必须回查MySQL中当前知识库的有效分片，防止已删除/越界向量进入上下文。检索原文只能放入 User 层的 `<retrieved_context>`，不能拼入 System Prompt。

## 11. AI 任务 `/ai/tasks`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/ai/tasks` | `ai:task:list` | 当前权限范围任务列表 |
| GET | `/ai/tasks/{id}` | `ai:task:query` | 任务进度/结果摘要/错误 |
| POST | `/ai/tasks/{id}/cancel` | `ai:task:cancel` | 设置 cancelRequested，由 Worker 安全停止 |
| POST | `/ai/tasks/{id}/retry` | `ai:task:retry` | 失败任务按幂等边界重试 |
| GET | `/ai/tasks/stream` | `ai:task:list` | SSE 推送当前用户任务变化 |

任务 SSE 可使用 `GET` 时，原生 EventSource 无法带 Authorization；仍建议复用 fetch 流封装，或未来增加一次性短期 stream ticket。禁止把长期 JWT 放查询参数。

## 12. AI 日志 `/ai/request-logs`（阶段 08 已实现）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/ai/request-logs/list` | `ai:log:list` | 按请求类型、业务、结果、时间分页 |
| GET | `/ai/request-logs/{id}` | `ai:log:query` | Token、耗时、请求 ID 和脱敏错误详情 |

日志不保存 Prompt、响应正文、Authorization 或 API Key。模型新增/修改操作日志关闭请求体和响应体保存；模型配置 Mapper 单独提升日志级别，避免开发环境 MyBatis DEBUG 输出密文绑定值。统计和导出作为工作台/成本分析增强，不在阶段 08 伪造接口。

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
