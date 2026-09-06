# CostAI 第一阶段数据库设计

> 状态：阶段 01 至阶段 11 已实现；工程造价知识库、文档解析、语义分片、Qdrant索引与带引用RAG已落地，报告等仍为设计稿。
> 数据库基线：MySQL、InnoDB、原生 MyBatis。  
> 已执行迁移：阶段 01～10 的版本化 SQL，最新为 `sql/ai_cost_chat_20260905.sql`；均已在本地 `ry-vue`（MySQL 8.0.11）执行并校验幂等。

## 1. 与现有若依表规范的对齐

当前 `BaseEntity` 只定义 `create_by/create_time/update_by/update_time/remark/params`，不包含主键和软删除字段。现有核心表使用 `del_flag char(1)`（`0` 正常、`2` 删除）而不是 `deleted`。

第一阶段业务表统一采用：

```text
id           bigint       not null auto_increment
create_by    varchar(64)   default ''
create_time  datetime
update_by    varchar(64)   default ''
update_time  datetime
remark       varchar(500)  default null
del_flag     char(1)       default '0'
```

- 实体继承 `BaseEntity`，主键和 `delFlag` 在具体实体声明。
- `create_by/update_by` 延续若依语义，保存登录名；需要稳定关联时另设 `*_user_id bigint`。
- 不引入数据库级级联删除。删除项目、文件、知识文档时由 Service 事务更新业务状态，并在事务提交后异步清理外部文件/向量。
- 所有 Mapper 查询默认带 `del_flag = '0'`。
- 金额 `decimal(20,2)`；数量和含量通常 `decimal(20,6)`；比率 `decimal(12,6)`，0.411800 表示 41.18%。
- JSON 字段保存快照/配置，不承载需要 SQL 汇总的金额。

## 2. 项目与文件

### 2.1 `cost_project` 项目档案

核心字段：

| 字段 | 类型 | 约束/说明 |
|---|---|---|
| id | bigint | 主键 |
| project_code | varchar(64) | 项目编号，`uk_cost_project_code` 唯一 |
| project_name | varchar(200) | 项目名称 |
| project_type | varchar(32) | 字典 `cost_project_type` |
| professional_type | varchar(32) | 专业字典 |
| project_stage | varchar(32) | 字典 `cost_project_stage` |
| construction_unit | varchar(200) | 建设单位 |
| contractor_unit | varchar(200) | 施工单位 |
| consulting_unit | varchar(200) | 咨询单位 |
| province/city/district | varchar(64) | 地区 |
| building_area | decimal(20,4) | 建筑面积，非负 |
| project_manager_id | bigint | 项目负责人，关联 `sys_user.user_id` |
| project_manager_name | varchar(100) | 负责人姓名快照 |
| owner_dept_id | bigint | 归属部门，数据权限字段 |
| submitted_amount | decimal(20,2) | 送审金额 |
| approved_amount | decimal(20,2) | 审定金额 |
| increase_amount | decimal(20,2) | 核增金额 |
| reduction_amount | decimal(20,2) | 核减金额 |
| reduction_rate | decimal(12,6) | 核减率，由 Java 计算 |
| start_date/completion_date | date | 开工/竣工日期 |
| project_status | varchar(32) | 项目状态字典 `cost_project_status` |
| description | varchar(2000) | 项目描述 |
| 公共字段 | - | 审计、备注、软删除 |

索引：`uk_cost_project_code(project_code)`、项目名称、类型、阶段、状态、负责人、归属部门和创建时间索引。

说明：项目编号在逻辑删除后仍保留唯一占用，以避免历史项目编号被重新解释。Service 在写入前给出友好重复提示，数据库唯一键处理并发兜底。核减率统一由 Java 以 `reduction_amount / submitted_amount`、6 位小数、`HALF_UP` 计算；送审金额为空或不大于 0 时返回 0。

阶段 01 同时创建四类字典：`cost_project_type`、`cost_project_stage`、`cost_project_status`、`cost_professional_type`，以及“造价管理/项目管理”和五项 RBAC 权限菜单。项目查询用 `owner_dept_id` 与 `project_manager_id` 接入现有数据权限切面。

### 2.2 `cost_project_file` 项目文件

阶段 02 已实现，迁移脚本为 `sql/cost_project_file_20260901.sql`：

| 字段 | 类型 | 约束/说明 |
|---|---|---|
| id | bigint | 自增主键 |
| project_id | bigint | 项目逻辑外键，所有访问先校验项目数据权限 |
| file_id | varchar(64) | 稳定业务标识，唯一键 |
| original_name/file_name | varchar(255) | 原文件名/UUID 存储文件名 |
| file_ext | varchar(16) | 归一化小写扩展名 |
| mime_type | varchar(128) | 上传 MIME，空值归一化为二进制类型 |
| file_size | bigint unsigned | 文件字节数 |
| storage_path | varchar(500) | 现有 profile 下私有相对路径，不返回前端 |
| file_category | varchar(32) | 字典 `cost_file_category` |
| file_hash | char(64) | SHA-256 摘要 |
| ai_parse_status | varchar(20) | 字典 `cost_ai_parse_status` |
| ai_parse_text | longtext | 后续解析结果，不在普通 API 返回 |
| ai_parse_error | varchar(2000) | 友好解析错误 |
| 公共字段 | - | 审计、备注、`del_flag` 逻辑删除 |

- 当前项目唯一存储实现是 RuoYi `profile` 本地文件系统，因此没有虚构 `storage_provider/bucket` 字段，也没有再创建一套存储系统。未来确定接入 MinIO/OSS 时再迁移为提供者配置与对象键。
- 文件二进制不进入数据库。私有路径固定在 `/profile/private/project/yyyy/MM/dd/`，下载只能走鉴权 Controller。
- 解析状态：`WAITING/PARSING/SUCCESS/FAILED/UNSUPPORTED`。DWG/DXF/IFC 初始为 `UNSUPPORTED`，其他允许格式为 `WAITING`，本阶段不伪造解析成功。
- 物理文件在逻辑删除事务提交后清理；上传元数据入库失败时立即清理已写文件。
- 项目存在未删除文件时拒绝删除项目，避免孤立元数据和物理文件。

索引：`uk_cost_project_file_file_id(file_id)`、`idx_cost_project_file_project(project_id)`、`idx_cost_project_file_category(project_id,file_category)`、`idx_cost_project_file_parse(project_id,ai_parse_status)`、`idx_cost_project_file_create_time(create_time)`。

## 3. BOQ 导入、清单与对比

阶段 03 只读取阶段 02 已保存的 `cost_project_file` 并在请求内返回有限预览。阶段 04 使用 `sql/cost_boq_import_20260901.sql` 创建以下三张已落地业务表，并幂等增加业务类型、导入状态字典及四项 RBAC 权限。

### 3.1 `cost_boq_batch` 导入批次（阶段 04 已实现）

核心字段：

`id/project_id/batch_name/business_type/source_file_id/sheet_name/header_row/field_mapping_json/professional_type/total_count/success_count/fail_count/total_amount/import_status/error_summary` + 公共字段。

- `business_type` 使用字典 `cost_boq_business_type`：`BOQ/CONTROL_PRICE/BID_PRICE/SUBMITTED/REVIEWED/SETTLEMENT/OTHER`。
- `import_status` 使用字典 `cost_boq_import_status`：`IMPORTING/SUCCESS/PARTIAL_FAILED/FAILED`。
- `field_mapping_json` 保存用户最终确认的“Excel列标识 → 标准字段”映射，不依赖预览建议再次推断。
- `total_amount decimal(24,6)` 汇总成功行的 Excel 原始合价；不使用 `double`。

索引：`idx_boq_batch_project(project_id,create_time)`、`idx_boq_batch_source_file(source_file_id)`、`idx_boq_batch_status(project_id,import_status)`。

### 3.2 `cost_boq_import_error` 导入错误明细（阶段 04 已实现）

字段：`id/project_id/batch_id/source_sheet/source_row/error_field/raw_value/error_message/raw_data_json` + 公共字段。错误行与成功行在同一批次中分别持久化，单行数值或长度错误不会让用户失去其他有效数据。

索引：`idx_boq_error_batch(batch_id,source_row)`、`idx_boq_error_project(project_id)`。错误文件导出和错误保留上限属于后续增强，当前真实错误行均可分页查询。

### 3.3 `cost_boq_item` 工程量清单（阶段 04 已实现）

字段：

`id/project_id/batch_id/sequence_no/item_code/item_name/item_feature/unit/quantity/unit_price/total_price/calculated_total_price/labor_price/material_price/machine_price/management_fee/profit/tax/professional_type/category/parent_id/item_level/source_sheet/source_row` + 公共字段。

类型：`quantity/unit_price` 为 `decimal(24,8)`；合价及费用为 `decimal(24,6)`；层级为 `int`。`total_price` 始终保存 Excel 原始合价，`calculated_total_price` 由 Java 以 `quantity × unit_price`、6 位小数、`HALF_UP` 另算，允许二者存在舍入差异。保留 `source_sheet/source_row` 以便定位原 Excel。

索引：

- `idx_boq_item_project(project_id)`
- `idx_boq_item_batch(batch_id,source_row)`
- `idx_boq_item_code(project_id,item_code)`
- `idx_boq_item_name(project_id,item_name(100))`
- `idx_boq_item_professional(project_id,professional_type)`

删除 `cost_boq_batch` 时，Service 在同一事务内按对比结果 → 错误行 → 清单明细 → 批次顺序逻辑删除；文件中心在仍有活动批次引用 `source_file_id` 时拒绝删除文件，从应用层避免孤儿数据。

### 3.4 `cost_boq_compare` 清单当前对比结果（阶段 05 已实现）

阶段 05 按明确需求采用单表保存一对左右批次的现行结果，不提前引入未要求的对比批次表。核心字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| project_id | bigint | 项目 ID |
| left_batch_id/right_batch_id | bigint | 左右导入批次 |
| left_item_id/right_item_id | bigint nullable | 左右清单；仅一侧状态时另一侧为空 |
| match_type | varchar(32) | `EXACT/HIGH_SIMILARITY/LOW_SIMILARITY/ONLY_LEFT/ONLY_RIGHT/MANUAL` |
| match_score | decimal(8,6) | 0 到 1 的匹配度 |
| quantity_diff | decimal(24,8) | 工程量左减右 |
| quantity_diff_rate | decimal(18,6) | 工程量差异 / 左值绝对值 |
| unit_price_diff | decimal(24,8) | 综合单价左减右 |
| unit_price_diff_rate | decimal(18,6) | 单价差异率 |
| total_price_diff | decimal(24,6) | Excel 原始合价左减右 |
| total_price_diff_rate | decimal(18,6) | 合价差异率 |
| 公共字段 | - | `create_by/create_time/update_by/update_time/remark/del_flag` |

差异与差异率全部由 Java `BigDecimal` 计算；左值为 0 或任一侧为空时差异率为 `null`，不伪造 0。重新匹配逻辑删除非人工结果并保留 `MANUAL`，批次行按 ID 顺序 `for update` 锁定以串行化同一批次组合的写操作。人工调整删除涉及所选左右清单的旧当前行，插入一对一人工行，再重建左右未匹配行。

索引：

- `idx_boq_compare_pair(project_id,left_batch_id,right_batch_id,match_type)`
- `idx_boq_compare_left_item(left_item_id)` / `idx_boq_compare_right_item(right_item_id)`
- `idx_boq_compare_left_batch(left_batch_id)` / `idx_boq_compare_right_batch(right_batch_id)`

当前表是现行匹配关系，不宣称保存审核历史。可复现审核证据由 `cost_review_task` 保存规则版本/配置快照，问题表保存对比数值与证据快照；后续再评估是否增加独立 `cost_boq_compare_batch`，禁止改写已执行的阶段 05 脚本。

### 3.5 匹配状态字典与权限（阶段 05 已实现）

- 字典 `cost_boq_match_type`：6 个状态，前端通过 `dict-tag` 展示，禁止硬编码中文状态。
- 权限：`cost:compare:list`、`cost:compare:start`、`cost:compare:manual`，均挂在现有项目管理菜单下作为按钮权限。

## 4. 审核

### 4.1 `cost_review_rule_config` 规则配置（阶段 06 已实现）

字段：`id/rule_code/rule_name/config_key/config_name/config_value/value_type/sort_num/description` + 公共字段。

- `(rule_code,config_key)` 唯一；迁移采用 `where not exists`，重复执行不覆盖管理员已经修改的阈值。
- 类型为 `BOOLEAN/DECIMAL/STRING_LIST`，Service 在更新时校验类型、非负、比例不大于 1，以及 `highRate >= warningRate/relativeTolerance`。
- 已落地 20 项配置：10 条规则启停、零单价豁免关键词、合价绝对/相对容差及高风险阈值、三类左右差异的预警/高风险阈值。

索引：唯一键 `uk_review_rule_config(rule_code,config_key)`，排序索引 `idx_review_rule_config_sort(rule_code,sort_num)`。

### 4.2 `cost_review_task` 审核任务（阶段 06/07 已实现）

字段：`id/project_id/left_batch_id/right_batch_id/task_name/status/rule_version/config_snapshot_json/left_item_count/right_item_count/compare_count/issue_count/medium_count/high_count/critical_count/risk_amount/started_by/start_time/finish_time/error_message` + 公共字段。

- 状态：`RUNNING/SUCCESS/FAILED`；阶段 06 同步执行纯 Java 规则，不虚构异步任务状态。
- `rule_version=JAVA_RULES_V1`，`config_snapshot_json` 保存本次全部规则配置，历史结果不受后续阈值修改影响。
- 创建 `RUNNING`、分块保存问题并完成、失败落状态分别使用短事务；规则运算阶段不占用数据库事务。

阶段 07 通过兼容迁移把阶段 06 的 `cost_review_batch` 原位改名为 `cost_review_task`，保留已有记录与自增主键，不建立第二套任务表。索引沿用项目/时间、批次对、左右批次和项目/状态索引。

### 4.3 `cost_review_issue` 审核问题（阶段 06/07/09 已实现）

字段：

`id/review_task_id/project_id/left_item_id/right_item_id/boq_item_id/compare_result_id/item_side/item_code_snapshot/item_name_snapshot/issue_type/issue_level/issue_title/issue_description/original_value/reference_value/difference_value/difference_rate/risk_amount/rule_code/evidence_json/ai_analysis/ai_suggestion/ai_confidence/ai_has_issue/ai_issue_type/ai_issue_level/ai_title/ai_model/ai_request_id/ai_analyzed_user_id/ai_analyzed_by/ai_analyzed_time/status/reviewer_user_id/reviewer/review_comment/review_time` + 公共字段。

- 风险级别：`INFO/LOW/MEDIUM/HIGH/CRITICAL`。
- 状态：`PENDING/CONFIRMED/IGNORED/RECTIFIED`，前端展示中文字典。
- 通用 value 字段保留展示字符串；可计算金额、差异和风险金额仍用 decimal 独立列。
- `evidence_json` 保存清单/对比 ID、原始数值、计算值和命中阈值，不保存不可追溯的模型思维过程。
- 阶段 09 使用已有 AI 预留列并补充结构化建议、模型请求和发起人审计字段；这些列均是 AI 建议快照，不覆盖原 `issue_type/issue_level/status`。
- 面向业务的问题类型为 `QUANTITY/UNIT_PRICE/TOTAL_PRICE/DUPLICATE/MISSING/NEW_ITEM/FEATURE/DATA/WRONG_ITEM/OTHER`；Java 规则通过 `rule_code` 保留具体来源，AI 可建议“疑似错项”。
- 人工处理后刷新任务风险统计；`reductionAmount` 是查询统计字段，只汇总已确认/已整改的正向合价差异，不重复落库。

索引：批次/级别/类型复合索引、项目/状态复合索引，以及清单、对比结果、批次/规则索引；阶段 09 增加 `idx_review_issue_ai_time(review_task_id,ai_analyzed_time)`。

## 5. AI 模型、Prompt、会话和日志

### 5.1 `ai_model_config`（阶段 08 已实现）

字段：`id/name/provider_type/base_url/api_key_encrypted/api_key_hint/chat_model/embedding_model/temperature/max_tokens/timeout_seconds/enabled/is_default` + 公共字段。

- 第一版 `provider_type=OPENAI_COMPATIBLE`，基础地址拼接 `/chat/completions` 和 `/embeddings`。
- `api_key_encrypted` 为 AES-256-GCM 密文，格式 `v1:Base64(12字节随机IV+密文+Tag)`；加密主密钥只由 `AI_CONFIG_ENCRYPTION_KEY` 环境变量提供。
- 列表/详情只返回 `api_key_hint`（`****` + 末四位），实体的密文字段使用 `@JsonIgnore`，操作日志不保存新增/修改请求体。
- `enabled` 延续若依 `0启用/1停用`，`is_default` 为 `Y/N`；模型调用可指定配置 ID，未指定时选择已启用的默认配置。
- `active_name` 是逻辑删除感知的生成列，仅活动配置名称唯一，已删除名称可以安全复用。

索引：`uk_ai_model_config_active_name(active_name)`、`idx_ai_model_config_route(enabled,is_default,id)`、`idx_ai_model_config_provider(provider_type,enabled)`。

### 5.2 `ai_prompt_template`

字段：`id/prompt_code/prompt_name/system_prompt/user_template/version/enabled` + 公共字段。

- 每行就是一个不可混淆的版本；同一 `prompt_code + version` 只有一条活动记录。
- 启用某版本时，事务内停用同编码的其他版本；业务查询按编码取得最高的已启用版本。
- `active_version_key` 是逻辑删除感知的生成列，保证活动版本唯一且允许删除后重建。
- 阶段 08 不把 Prompt 硬编码进 Java；草稿/发布/回滚工作流和缓存作为后续增强。

索引：`uk_ai_prompt_active_version(active_version_key)`、`idx_ai_prompt_active(prompt_code,enabled,version)`、`idx_ai_prompt_name(prompt_name)`。

### 5.3 `ai_conversation`（阶段 10 已实现）

字段：`id/user_id/title/mode/project_id/project_name/message_count/last_message_time/generating` + 公共字段（无 `remark`）。

- `mode`：`GENERAL/PROJECT`；项目模式必须关联一个通过现有数据权限校验的 `cost_project`。
- `generating` 使用条件更新领取生成权，阻止同一会话并发生成；完成或失败均释放。
- 会话只能按 `user_id` 查询、修改和逻辑删除，不提供跨用户万能接口。

索引：`idx_ai_conversation_user_time(user_id,del_flag,last_message_time)`、`idx_ai_conversation_project(project_id,del_flag)`。

### 5.4 `ai_message`（阶段 10 已实现）

字段：`id/conversation_id/user_id/role/content/status/model/request_id/sources_json/tool_calls_json/token_usage_json/error_message` + 公共字段（无 `remark`）。

- `role`：`USER/ASSISTANT`；System Prompt 从版本化 `ai_prompt_template` 读取，不作为普通历史消息落库。
- `status`：`STREAMING/COMPLETED/FAILED`。用户消息和AI占位消息在请求开始时落库，断线后可查询恢复已保存状态。
- 来源、受控工具调用摘要和Token用量分别保存为JSON；阶段10尚无知识分片，因此不提前创建空的来源子表。
- Mapper日志级别单独提升为INFO，防止开发环境打印消息正文和项目上下文绑定值。

索引：`idx_ai_message_conversation(conversation_id,del_flag,id)`、`idx_ai_message_user_time(user_id,del_flag,create_time)`。

### 5.5 `ai_request_log`（阶段 08 已实现）

字段：`id/user_id/model_config_id/provider_type/model_name/request_type/business_type/business_id/request_id/prompt_tokens/completion_tokens/total_tokens/duration_ms/success/error_code/error_message` + 公共字段。

- 不记录 API Key；Prompt/响应正文默认也不进入日志表，必要审计由消息表或脱敏快照承担。
- `request_type`：`CHAT/STREAM_CHAT/STRUCTURED_CHAT/EMBEDDING/CONNECTION_TEST`；`success` 使用 `Y/N`。
- 索引：`idx_ai_request_log_user(user_id,create_time)`、`idx_ai_request_log_model(model_config_id,create_time)`、`idx_ai_request_log_business(business_type,business_id)`、`idx_ai_request_log_success(success,create_time)`、`idx_ai_request_log_type(request_type,create_time)`。

## 6. AI 任务中心

### `ai_task`

字段：`id/task_type/business_type/business_id/task_name/status/progress/total_count/success_count/fail_count/result_json/error_code/error_message/retry_count/max_retry_count/lock_owner/lock_time/start_time/finish_time/cancel_requested/requested_by` + 公共字段。

- 状态：`WAITING/RUNNING/SUCCESS/FAILED/CANCELLED`。
- `progress` 为 `decimal(5,2)`，范围 0~100。
- Worker 领取任务时使用条件更新/乐观锁，防止多实例重复执行。
- 任务只保存结果摘要和业务结果 ID，不塞入大型结果正文。

索引：`idx_task_status(status,create_time)`、`idx_task_business(business_type,business_id)`、`idx_task_requester(requested_by,create_time)`、`idx_task_lock(status,lock_time)`。

## 7. 知识库与向量引用

### 7.1 `knowledge_base`（阶段 11 已实现）

字段：`id/name/description/embedding_model/vector_store/vector_collection/status/document_count/chunk_count/top_k/similarity_threshold/max_context_chars` + 公共字段。

- 主库为 MySQL，向量本体进入 Qdrant；集合按 `costai_kb_{id}` 隔离。
- `top_k` 为 1~20，`similarity_threshold` 为 0~1，`max_context_chars` 为 1,000~50,000；当前按字符做可验证的硬上限，不伪称精确 Token 计数。
- 索引：逻辑删除感知的 `uk_knowledge_base_active_name(active_name)`、`idx_knowledge_base_status(status,del_flag)`。

### 7.2 `knowledge_document`（阶段 11 已实现）

字段：`id/knowledge_base_id/project_file_id/document_name/document_type/parse_status/chunk_count/char_count/content_hash/embedding_model/vector_collection/error_message/indexed_time` + 公共字段。

- `project_file_id` 复用 `cost_project_file`，不重复保存文件本体；项目文件删除前检查知识文档引用。
- 状态：`WAITING/PARSING/SUCCESS/FAILED/OCR_REQUIRED`。无文本扫描 PDF 进入 `OCR_REQUIRED`，不静默生成空索引。
- 索引：逻辑删除感知的知识库+项目文件唯一键，以及知识库/状态、项目文件引用索引。

### 7.3 `knowledge_chunk`（阶段 11 已实现）

字段：`id/knowledge_base_id/document_id/content/page_number/section_title/chunk_index/char_count/content_hash/vector_store/vector_collection/vector_id/metadata_json/index_status` + 公共字段。

- 向量本体写 Qdrant；MySQL 只保存 `vector_id` 和可追溯文本。
- `content` 是 RAG 原始证据，不是可信指令。

索引：`idx_knowledge_chunk_document(document_id,del_flag,chunk_index)`、`idx_knowledge_chunk_base_status(knowledge_base_id,index_status,del_flag)`、`idx_knowledge_chunk_vector(vector_collection,vector_id)`。

## 8. 报告

### `cost_report`

字段：`id/project_id/report_type/report_name/version_no/status/template_code/statistics_snapshot_json/ai_content_json/project_file_id/generated_by/generated_time/error_message` + 公共字段。

- `statistics_snapshot_json` 由 Java 生成并冻结；模型只能解释，不自行生成数字。
- 生成的 Word/PDF 作为 `cost_project_file` 保存，`project_file_id` 指向主输出文件；多格式可后续拆子表。
- 状态：`DRAFT/GENERATING/READY/FAILED/ARCHIVED`。

索引：`idx_report_project(project_id,report_type)`、`uk_report_version(project_id,report_type,version_no)`、`idx_report_status(status)`。

## 9. 字典与菜单数据

优先使用现有 `sys_dict_type/sys_dict_data`，不新建枚举配置表。MVP 首批字典：

- `cost_project_type`
- `cost_professional_type`
- `cost_project_stage`
- `cost_project_status`
- `cost_file_category`
- `cost_boq_business_type`
- `cost_boq_import_status`
- `cost_boq_match_type`
- `cost_issue_type`
- `cost_issue_level`
- `cost_issue_status`
- `ai_task_status`
- `knowledge_parse_status`
- `ai_provider_type`

菜单与按钮继续写 `sys_menu`，不创建第二套菜单表。

## 10. 迁移顺序

1. `V1`：字典、一级菜单、项目/文件表。
2. `V1.1`：BOQ 预览权限（阶段 03，`sql/cost_boq_preview_20260901.sql`，无新增业务表）。
3. `V2`：BOQ 导入批次、错误、清单（阶段 04，`sql/cost_boq_import_20260901.sql`，已执行并验证幂等）。
4. `V2.1`：BOQ 当前对比结果、匹配状态和权限（阶段 05，`sql/cost_boq_compare_20260901.sql`，已执行并验证幂等）。
5. `V3`：规则配置、审核批次/问题、字典和权限（阶段 06，`sql/cost_review_rule_20260901.sql`，已执行并验证幂等）。
6. `V3.1`：审核任务兼容改名、问题人工处理字段/分类字典/权限（阶段 07，`sql/cost_review_issue_manage_20260901.sql`，已执行并验证幂等）。
7. `V4`：模型配置、Prompt 和请求日志（阶段 08，`sql/ai_llm_infrastructure_20260902.sql`，已执行并验证幂等）；任务中心单独进入后续迁移。
8. `V4.1`：审核问题AI结构化建议字段、默认 `COST_REVIEW_AGENT` Prompt、疑似错项字典和分析权限（阶段 09，`sql/cost_review_agent_20260905.sql`，已执行并验证幂等）。
9. `V5`：AI造价助手会话、消息、Prompt、字典和菜单（阶段 10，`sql/ai_cost_chat_20260905.sql`，已在本地执行；重复执行保持幂等）。
10. `V6`：知识库/文档/分片、字典、Prompt与菜单（阶段 11，`sql/cost_knowledge_20260906.sql`，已在本地连续执行两次）。
11. `V7`：报告与工作台必要索引。

当前项目没有 Flyway/Liquibase。第一阶段先沿用 `sql/` 版本化脚本，文件名带日期/序号且禁止修改已执行脚本；是否引入迁移工具应作为单独架构决策，不能夹带在业务功能中。

## 11. 数据一致性与事务

- 项目新增/编辑、字段映射确认、问题状态变更：短事务。
- Excel：批次创建短事务；解析在事务外；每 500 条独立批量事务；最终汇总短事务。
- 清单对比：同一事务锁定左右批次、逻辑删除待替换结果、批量写入新结果；无外部网络调用，人工结果重匹配时保留。
- AI/Embedding/Qdrant/文件网络调用：不得持有数据库事务。
- 知识解析/Embedding/Qdrant 请求在数据库事务外执行；解析完成后分片按最多500条写入，Embedding每批32段。
- 知识文档删除：短事务逻辑删除关联记录，提交后删除 Qdrant 文档向量；原项目文件不删除。若外部清理失败，检索结果仍须回查 MySQL 有效分片，避免孤立向量被引用；持久化重试将在 AI 任务中心补齐。
- 报告：统计快照先落库提交，再调用 AI，再生成文件，逐阶段更新状态。
- 任何自动重试都必须具备幂等键：导入批次 ID、审核批次 ID、文档 ID 或报告 ID。
