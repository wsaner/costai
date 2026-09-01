# CostAI 第一阶段数据库设计

> 状态：阶段 01 已生成项目管理 DDL；其余表仍为设计稿。  
> 数据库基线：MySQL、InnoDB、原生 MyBatis。  
> 阶段 01 迁移脚本：`sql/cost_project_20260901.sql`；已在本地配置的 `ry-vue`（MySQL 8.0.11）执行并校验。

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

字段：`id/project_id/file_category/original_name/file_extension/mime_type/file_size/storage_provider/storage_bucket/storage_key/file_hash/upload_user_id/parse_status/parsed_text/parse_error/reserved_file_type` + 公共字段。

- `storage_provider` 第一版为 `LOCAL`，以后支持 `MINIO/OSS`。
- `storage_key` 是定位文件的稳定键；不把二进制写数据库。
- `parsed_text` 为 `longtext`，只保存可控大小的标准化文本；超大文档正文由分片表承载，避免重复存储。
- `parse_status`：`WAITING/RUNNING/SUCCESS/FAILED/UNSUPPORTED`。

索引：`idx_project_file_project(project_id)`、`idx_project_file_category(project_id,file_category)`、`idx_project_file_parse(parse_status)`、`idx_project_file_hash(file_hash)`。

## 3. BOQ 导入、清单与对比

### 3.1 `cost_boq_import_batch` 导入批次

字段：

`id/project_id/project_file_id/document_type/data_role/status/header_row_no/source_sheet_count/field_mapping_json/mapping_confidence/total_count/success_count/fail_count/error_summary/confirmed_by/confirmed_time/start_time/finish_time` + 公共字段。

- `document_type`：`BOQ/CONTROL_PRICE/BID_PRICE/SUBMITTED_BUDGET/REVIEWED_BUDGET/SETTLEMENT`。
- `data_role`：`SUBMITTED/REVIEWED/REFERENCE`，用于两表对比语义。
- `status`：`UPLOADED/MAPPING/WAIT_CONFIRM/IMPORTING/SUCCESS/PARTIAL_FAILED/FAILED/CANCELLED`。
- 字段映射 JSON 保存原始列名、目标字段、置信度、识别来源和用户修正结果。

索引：`idx_boq_import_project(project_id)`、`idx_boq_import_file(project_file_id)`、`idx_boq_import_status(status)`。

### 3.2 `cost_boq_import_error` 导入错误明细

字段：`id/import_batch_id/source_sheet/source_row/error_code/error_field/raw_value/error_message` + 公共字段。

索引：`idx_import_error_batch(import_batch_id, source_sheet, source_row)`。错误明细设保留上限，超限只累计计数并生成可下载错误文件，防止单次坏文件写入海量错误记录。

### 3.3 `cost_boq_item` 工程量清单

字段：

`id/project_id/import_batch_id/source_file_id/data_role/sequence_no/item_code/item_name/item_feature/unit/quantity/unit_price/total_price/labor_price/material_price/machine_price/management_fee/profit/regulatory_fee/tax/professional_type/category/parent_id/item_level/source_sheet/source_row/normalized_name/normalized_feature/content_hash` + 公共字段。

类型：所有金额为 `decimal(20,2)`；`quantity` 为 `decimal(20,6)`；层级为 `int`。保留 `source_sheet/source_row` 以便定位原 Excel。

索引：

- `idx_boq_project(project_id)`
- `idx_boq_batch(import_batch_id)`
- `idx_boq_project_code(project_id,item_code)`
- `idx_boq_project_role(project_id,data_role)`
- `idx_boq_parent(parent_id)`
- `idx_boq_hash(project_id,content_hash)`

### 3.4 `cost_boq_compare_batch` 对比批次

字段：`id/project_id/source_batch_id/target_batch_id/status/algorithm_version/config_json/source_count/target_count/matched_count/missing_count/added_count/start_time/finish_time/error_message` + 公共字段。

索引：`idx_compare_project(project_id)`、`idx_compare_source(source_batch_id)`、`idx_compare_target(target_batch_id)`、`idx_compare_status(status)`。

### 3.5 `cost_boq_compare_result` 对比结果

字段：

`id/compare_batch_id/project_id/source_item_id/target_item_id/match_type/match_score/match_reason/source_quantity/target_quantity/quantity_difference/quantity_difference_rate/source_unit_price/target_unit_price/unit_price_difference/unit_price_difference_rate/source_total_price/target_total_price/total_difference/total_difference_rate/result_type/review_required` + 公共字段。

- `match_type`：`CODE/EXACT_TEXT/RULE_SIMILARITY/EMBEDDING/LLM/MANUAL/UNMATCHED`。
- `result_type`：`MATCHED/MISSING/ADDED/AMBIGUOUS`。
- 对比值使用快照，避免原清单调整后破坏历史审核证据。

索引：`idx_compare_result_batch(compare_batch_id)`、`idx_compare_result_source(source_item_id)`、`idx_compare_result_target(target_item_id)`、`idx_compare_result_type(compare_batch_id,result_type)`。

## 4. 审核

### 4.1 `cost_review_batch` 审核批次

字段：`id/project_id/compare_batch_id/task_id/status/rule_config_json/rule_version/issue_count/high_risk_count/risk_amount/started_by/start_time/finish_time/error_message` + 公共字段。

状态：`WAITING/RUNNING/SUCCESS/PARTIAL_FAILED/FAILED/CANCELLED`。规则配置和版本必须快照保存，保证结果可复现。

索引：`idx_review_project(project_id)`、`idx_review_compare(compare_batch_id)`、`idx_review_task(task_id)`、`idx_review_status(status)`。

### 4.2 `cost_review_issue` 审核问题

字段：

`id/review_batch_id/project_id/boq_item_id/compare_result_id/issue_type/issue_level/issue_title/issue_description/original_value/reference_value/difference_value/difference_rate/risk_amount/ai_analysis/ai_suggestion/ai_confidence/status/reviewer_user_id/review_comment/review_time/rule_code/evidence_json` + 公共字段。

- 风险级别：`INFO/LOW/MEDIUM/HIGH/CRITICAL`。
- 状态：`PENDING/CONFIRMED/IGNORED/RECTIFIED`，前端展示中文字典。
- 通用 value 字段保留展示字符串；可计算金额、差异和风险金额仍用 decimal 独立列。
- `evidence_json` 保存规则输入、规范引用和历史样本引用，不保存不可追溯的模型思维过程。

索引：`idx_issue_project(project_id)`、`idx_issue_batch(review_batch_id)`、`idx_issue_boq(boq_item_id)`、`idx_issue_level(project_id,issue_level)`、`idx_issue_status(project_id,status)`、`idx_issue_type(project_id,issue_type)`。

## 5. AI 模型、Prompt、会话和日志

### 5.1 `ai_provider`

字段：`id/provider_name/provider_type/base_url/api_key_ciphertext/api_key_iv/key_version/api_key_hint/model_name/embedding_model/temperature/max_tokens/enabled/priority/timeout_seconds/extra_config_json` + 公共字段。

- `provider_name` 业务唯一。
- 列表/详情 VO 只返回 `api_key_hint`（如 `sk-****abcd`），绝不返回密文和 IV。
- `enabled` 用 `char(1)` 延续若依状态风格；选择 Provider 时按能力、启用状态和 priority 路由。

索引：`uk_ai_provider_name(provider_name,del_flag)`、`idx_ai_provider_route(enabled,priority)`。

### 5.2 `ai_prompt_template`

字段：`id/prompt_code/prompt_name/business_type/active_version_id/enabled` + 公共字段。

索引：`uk_prompt_code(prompt_code,del_flag)`、`idx_prompt_enabled(enabled)`。

### 5.3 `ai_prompt_version`

字段：`id/template_id/version_no/system_prompt/user_prompt_template/model_override/temperature/json_schema/config_json/status/published_by/published_time` + 公共字段。

- `status`：`DRAFT/PUBLISHED/RETIRED`。
- 同一模板版本号唯一：`uk_prompt_version(template_id,version_no)`。
- 模板发布和 active version 切换在短事务内完成，变更后清理 Redis 缓存。

### 5.4 `ai_chat_session`

字段：`id/user_id/project_id/knowledge_base_id/title/session_type/last_message_time/message_count/status` + 公共字段。

索引：`idx_chat_user(user_id,last_message_time)`、`idx_chat_project(project_id)`。

### 5.5 `ai_chat_message`

字段：`id/session_id/parent_message_id/role/content/structured_content_json/model/provider_id/prompt_tokens/completion_tokens/total_tokens/status/error_message/sequence_no` + 公共字段。

- `role`：`SYSTEM/USER/ASSISTANT/TOOL`，数据库不把检索内容升级为 SYSTEM。
- `structured_content_json` 对应 `answer/sources/suggestions/toolCalls/tokenUsage`。

索引：`idx_message_session(session_id,sequence_no)`、`idx_message_parent(parent_message_id)`。

### 5.6 `ai_message_source`

字段：`id/message_id/source_type/business_id/document_id/chunk_id/source_name/page_number/section_title/quote_text/score/metadata_json` + 公共字段。

索引：`idx_source_message(message_id)`、`idx_source_chunk(chunk_id)`。`quote_text` 只保存用于回答的短摘录，完整原文仍由文档/分片管理。

### 5.7 `ai_request_log`

字段：`id/user_id/business_type/business_id/provider_id/provider_name/model/request_id/prompt_tokens/completion_tokens/total_tokens/duration_ms/success/error_code/error_message` + 公共字段。

- 不记录 API Key；Prompt/响应正文默认也不进入日志表，必要审计由消息表或脱敏快照承担。
- 索引：`idx_ai_log_user(user_id,create_time)`、`idx_ai_log_business(business_type,business_id)`、`idx_ai_log_provider(provider_id,create_time)`、`idx_ai_log_success(success,create_time)`。

## 6. AI 任务中心

### `ai_task`

字段：`id/task_type/business_type/business_id/task_name/status/progress/total_count/success_count/fail_count/result_json/error_code/error_message/retry_count/max_retry_count/lock_owner/lock_time/start_time/finish_time/cancel_requested/requested_by` + 公共字段。

- 状态：`WAITING/RUNNING/SUCCESS/FAILED/CANCELLED`。
- `progress` 为 `decimal(5,2)`，范围 0~100。
- Worker 领取任务时使用条件更新/乐观锁，防止多实例重复执行。
- 任务只保存结果摘要和业务结果 ID，不塞入大型结果正文。

索引：`idx_task_status(status,create_time)`、`idx_task_business(business_type,business_id)`、`idx_task_requester(requested_by,create_time)`、`idx_task_lock(status,lock_time)`。

## 7. 知识库与向量引用

### 7.1 `knowledge_base`

字段：`id/name/description/embedding_provider_id/embedding_model/status/document_count/chunk_count/top_k/similarity_threshold/max_context_tokens` + 公共字段。

索引：`uk_knowledge_name(name,del_flag)`、`idx_knowledge_status(status)`。

### 7.2 `knowledge_document`

字段：`id/knowledge_base_id/project_file_id/document_name/document_type/parse_status/chunk_count/token_count/content_hash/error_message/indexed_time` + 公共字段。

索引：`idx_document_base(knowledge_base_id)`、`idx_document_file(project_file_id)`、`idx_document_status(parse_status)`、`idx_document_hash(knowledge_base_id,content_hash)`。

### 7.3 `knowledge_chunk`

字段：`id/knowledge_base_id/document_id/content/page_number/section_title/chunk_index/token_count/content_hash/vector_store/vector_collection/vector_id/metadata_json/index_status` + 公共字段。

- 向量本体写 Qdrant；MySQL 只保存 `vector_id` 和可追溯文本。
- `content` 是 RAG 原始证据，不是可信指令。

索引：`uk_chunk_order(document_id,chunk_index)`、`idx_chunk_base(knowledge_base_id)`、`idx_chunk_document(document_id)`、`idx_chunk_vector(vector_store,vector_id)`、`idx_chunk_status(index_status)`。

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
- `cost_boq_document_type`
- `cost_import_status`
- `cost_issue_type`
- `cost_issue_level`
- `cost_issue_status`
- `ai_task_status`
- `knowledge_parse_status`
- `ai_provider_type`

菜单与按钮继续写 `sys_menu`，不创建第二套菜单表。

## 10. 迁移顺序

1. `V1`：字典、一级菜单、项目/文件表。
2. `V2`：BOQ 导入批次、错误、清单。
3. `V3`：对比批次/结果、审核批次/问题。
4. `V4`：Provider、Prompt、任务、请求日志。
5. `V5`：聊天会话/消息/来源。
6. `V6`：知识库/文档/分片。
7. `V7`：报告与工作台必要索引。

当前项目没有 Flyway/Liquibase。第一阶段先沿用 `sql/` 版本化脚本，文件名带日期/序号且禁止修改已执行脚本；是否引入迁移工具应作为单独架构决策，不能夹带在业务功能中。

## 11. 数据一致性与事务

- 项目新增/编辑、字段映射确认、问题状态变更：短事务。
- Excel：批次创建短事务；解析在事务外；每 500~1000 条独立批量事务；最终汇总短事务。
- AI/Embedding/Qdrant/文件网络调用：不得持有数据库事务。
- 知识文档删除：先标记删除并创建清理任务；任务删除 Qdrant 向量和物理文件，失败可重试。
- 报告：统计快照先落库提交，再调用 AI，再生成文件，逐阶段更新状态。
- 任何自动重试都必须具备幂等键：导入批次 ID、审核批次 ID、文档 ID 或报告 ID。
