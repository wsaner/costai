# CostAI 平台架构与落地规划

> 基线日期：2026-09-01  
> 当前进度：阶段 0 扫描完成；阶段 01 至阶段 10 均已实现并验证，当前已具备项目/文件/BOQ导入匹配、规则与AI审核、统一LLM基础设施，以及带项目有限上下文的SSE造价聊天闭环。
> 维护规则：后续每次开发前先阅读本文件及同目录的数据库、API、任务文档，并检查 Git 状态、最近提交和未完成任务。

## 一、项目整体结构

当前仓库是 RuoYi-Vue 3.9.2 的 Spring Boot 3 分支，不是新建的空项目。后端为 Maven 聚合式模块化单体，前端是独立的 Vue 3 应用。

```text
cost-ai/
├─ pom.xml                         Maven 父工程，JAR 版本统一管理
├─ ruoyi-admin/                    Web 启动模块、Controller、运行配置
├─ ruoyi-framework/                Security、JWT、数据权限、异常、Web 配置
├─ ruoyi-system/                   用户/角色/菜单/部门/岗位/字典等系统服务
├─ ruoyi-common/                   BaseEntity、返回体、分页、Redis、文件、Excel 工具
├─ ruoyi-quartz/                   Quartz 定时任务与日志
├─ ruoyi-generator/                CRUD 代码生成器
├─ ruoyi-ui/                       Vue 3 + Vite 管理端
├─ sql/                            MySQL 初始化脚本与 Quartz 脚本
├─ doc/                            原有若依使用手册
└─ docs/                           CostAI 持续维护文档
```

模块依赖主链为：`ruoyi-admin -> ruoyi-framework -> ruoyi-system -> ruoyi-common`；`ruoyi-admin` 同时引入 `ruoyi-quartz`、`ruoyi-generator`。这是模块化单体，不应在 MVP 阶段引入 Spring Cloud、Gateway、Nacos、MQ 或 Kubernetes。

### 当前仓库状态

- `F:\codex\cost-ai` 当前 Git 元数据已可用。阶段 07 开始时最近提交仍为 `f29402f 截止到4阶段`；阶段 05/06 尚未提交的代码与文档修改已保留并继续增量维护。
- 后续每阶段继续按统一规则检查状态、diff、最近提交和未完成任务，不覆盖用户未提交改动。
- `ruoyi-cost` 与 `ruoyi-admin` 已建立 JUnit 5 测试；截至阶段 07 两模块共有 86 项测试，新增覆盖问题状态机、人工处理、统计刷新、Mapper 和权限契约。

## 二、后端技术栈

| 项目 | 实际情况 | 对 CostAI 的结论 |
|---|---|---|
| Java | 父 POM 指定 Java 17 | 沿用 Java 17，不降级、不升级大版本 |
| Spring Boot | 3.5.16 | 保持当前版本，不随意升级 |
| 构建 | Maven 3.9.10，多模块 | 新增单一业务模块 `ruoyi-cost` |
| Web | Spring MVC / Tomcat | SSE 使用 MVC `SseEmitter`；不迁移响应式服务器 |
| ORM | 原生 MyBatis 3.0.5 | 沿用 Mapper 接口 + XML，不引入 MyBatis-Plus |
| 分页 | PageHelper 2.1.1 | 沿用 `startPage()` + `TableDataInfo` |
| 数据库 | MySQL + Druid 1.2.28 | 金额使用 `decimal` / Java `BigDecimal` |
| 缓存 | Spring Data Redis + Lettuce | 复用 `RedisCache`，只缓存配置/热点数据 |
| 安全 | Spring Security + JWT + Redis 会话 | 所有新接口接入 `@PreAuthorize` 和现有 Token |
| API 文档 | springdoc-openapi 2.9.0 | 复用 OpenAPI；已增加独立 `cost` 分组扫描 `/cost/**` Controller |
| Excel | Apache POI 5.5.1、自研 `ExcelUtil` | 常规导入导出复用；10 万行 BOQ 导入另加流式读取能力 |
| 定时/异步 | Quartz + Spring `ThreadPoolTaskExecutor` | Quartz 适合计划任务；AI 长任务另建持久化任务执行器 |
| 异常 | `GlobalExceptionHandler` + `ServiceException` | 新业务异常统一转换为友好消息，不返回堆栈 |
| 操作日志 | `@Log` + 异步 `SysOperLog` | CRUD/导入/审核/报告操作直接复用 |

### 后端代码规范基线

- Controller 继承 `BaseController`，使用 `AjaxResult`、`TableDataInfo`、`toAjax()`。
- 列表分页使用 `startPage()`，Mapper 返回 `List<T>`，PageHelper 计算总数。
- Service 使用接口 + `impl` 实现；复杂业务按职责拆分，不把审核逻辑堆入单一 Service。
- Mapper 使用 Java 接口 + `resources/mapper/**/**Mapper.xml`。
- 实体继承 `BaseEntity`，直接复用 `createBy/createTime/updateBy/updateTime/remark/params`。
- 当前软删除字段惯例是 `del_flag char(1)`，值 `0` 表示存在、`2` 表示删除；不是 `deleted` 布尔值。CostAI 表沿用这一惯例。
- 数据主键普遍为 MySQL `bigint auto_increment`。第一阶段继续使用，不引入另一套 ID 生成器。
- 数据权限由 `@DataScope` 切面向 `BaseEntity.params.dataScope` 注入 SQL，支持全部、自定义部门、本部门、本部门及子部门、仅本人。

### 构建环境风险

- 命令行 `java -version` 指向 JDK 17。
- 但 `JAVA_HOME=E:\Java\jdk1.8.0_201`，所以 `mvn -version` 实际使用 Java 8；当前 Spring Boot 3/Java 17 工程无法在该 Maven Java 环境下可靠构建。
- 阶段 0 已通过命令级临时设置 `JAVA_HOME=E:\Java\openjdk-17_windows-x64_bin\jdk-17` 完成后端 `mvn package` 和 `mvn test`，两次均为 `BUILD SUCCESS`；`mvn test` 同时确认当前所有模块均为 `No tests to run`。
- 系统级 `JAVA_HOME` 仍指向 JDK 8。开始业务开发前仍应修正默认环境，避免每次构建依赖临时覆盖。此项是环境修正，不是项目依赖升级。

## 三、前端技术栈

| 项目 | 实际情况 | 对 CostAI 的结论 |
|---|---|---|
| Vue | 3.5.26 | 复用 Composition API 与 `<script setup>` |
| 脚本语言 | JavaScript；`src` 内没有 `.ts/.tsx` | MVP 继续使用 JavaScript，不做全量 TS 迁移 |
| 构建 | Vite 6.4.1 | 沿用现有环境变量和 `/dev-api` 代理 |
| UI | Element Plus 2.13.1 | 系统 CRUD 复用现有风格，AI 页面在其上做产品化布局 |
| 状态管理 | Pinia 3.0.4 | 复用现有 store 模式 |
| 路由 | Vue Router 4.6.4 | 菜单路由继续由后端 `sys_menu` 动态下发 |
| HTTP | Axios 1.13.2 | 普通 API 复用 `src/utils/request.js` |
| 图表 | ECharts 5.6.0 | 工作台趋势/问题分类直接复用 |
| 上传 | 全局 `FileUpload`、`ImageUpload`、`ExcelImportDialog` | 复用交互与鉴权方式，CostAI 文件中心增加业务专用封装 |
| 富文本 | Vue Quill | 可用于报告编辑，不等同 Markdown 渲染 |
| Markdown | 未安装 | AI 聊天需新增 Markdown 渲染与 XSS 清洗依赖 |
| SSE/WebSocket | 未发现实现 | 新增带 Authorization 的 fetch 流读取封装 |

### 前端代码规范基线

- API 按业务目录放在 `src/api/...`，统一调用 `request()`。
- 页面放在 `src/views/...`，路由组件名称必须与 `sys_menu.component` 一致。
- 后端返回动态路由，Pinia `permission` store 使用 `import.meta.glob('./../../views/**/*.vue')` 加载页面。
- 按钮权限复用 `v-hasPermi`，路由权限复用菜单与 permission store。
- 全局已有分页、字典标签、文件上传、图片上传、富文本、工具栏组件。
- Axios 默认超时 10 秒，不适合 AI 长响应。流式聊天不能直接沿用默认 Axios 实例；应使用封装后的 `fetch + ReadableStream`，保留 Bearer Token 和统一错误映射。

## 四、认证授权体系

1. 登录由 Spring Security `AuthenticationManager` 完成，密码使用 BCrypt。
2. `TokenService` 生成 JWT；JWT 中保存随机 UUID 和用户名，完整 `LoginUser`、角色和权限存 Redis。
3. 请求携带 `Authorization: Bearer <token>`，`JwtAuthenticationTokenFilter` 从 Redis恢复身份并刷新有效期。
4. 后端接口使用 `@PreAuthorize("@ss.hasPermi('...')")` 做细粒度权限控制。
5. 菜单/路由来自 `sys_menu`，前端按钮通过 `v-hasPermi` 控制。
6. 数据范围通过角色 `data_scope` + `@DataScope` 实现。CostAI 项目表保存 `owner_dept_id` 与 `project_manager_id`，查询别名传给现有切面即可实现“本人/本部门/部门及子部门/全部”。

### CostAI 权限接入原则

- 不新增用户、角色或会话体系。
- 不关闭 Security，不提供 admin 万能业务接口。
- 新权限统一使用 `cost:*` 或 `ai:*`，完整清单见 `AI_COST_API.md`。
- Tool Calling 必须在每次工具执行前校验当前用户对项目、知识库及具体动作的权限；模型永远不能直接执行 SQL。
- SSE 流请求也必须鉴权。因为原生 `EventSource` 不能方便地设置 Authorization Header，聊天采用 `POST + fetch` 流读取，而不是把 JWT 放到 URL。

## 五、数据库访问方式

- MySQL 主库，Druid 连接池；从库配置存在但默认关闭。
- 原生 MyBatis XML 映射，`@MapperScan("com.ruoyi.**.mapper")` 已覆盖新增 `com.ruoyi.cost...mapper`。
- PageHelper 方言为 MySQL。
- 现有 SQL 通过 `params.dataScope` 拼接数据权限条件，切面会先清空该值以降低注入风险。
- 现有初始化脚本没有显式外键约束，业务关系由索引和应用服务维护。CostAI 为减少迁移/级联风险，第一阶段同样使用逻辑外键并建立索引，不启用数据库级级联删除。
- 金额统一 `decimal(20,2)`，数量/面积 `decimal(20,6)` 或按字段精度定义；Java 全部使用 `BigDecimal`。差异率建议存 `decimal(12,6)`，展示层再格式化百分比。
- AI 结构化内容、元数据和统计快照使用 MySQL `json`；超长解析文本、Prompt 和消息正文使用 `longtext`。

详细表设计见 `AI_COST_DATABASE.md`。

## 六、文件体系

### 已有能力

- `RuoYiConfig.profile` 当前配置为 `D:/ruoyi/uploadPath`。
- `/common/upload` 和 `/common/uploads` 使用 `FileUploadUtils` 写本地文件系统，数据库只保存资源路径。
- `/profile/**` 映射到本地 profile 目录，可下载/展示资源。
- 默认后端白名单支持图片、Word、Excel、PPT、TXT、压缩包、视频、PDF，但不含 CSV。
- Spring multipart 当前限制：单文件 10 MB、单请求 20 MB；`FileUploadUtils` 内部上限 50 MB；前端通用 `FileUpload` 默认 5 MB。

### 能复用与不能直接复用的边界

- 复用：路径规范、文件名生成、基础扩展名校验、资源映射、Bearer Token 上传方式。
- 不能直接把 `/common/upload` 当项目文件中心：它不记录项目/分类/上传人/哈希/解析状态，也没有项目数据权限。
- 阶段 02 已通过专用 `/cost/project/{id}/files` 接口补齐业务元数据与项目访问校验，底层直接复用现有 `profile`、`FileUploadUtils` 和 `FileUtils`。由于仓库只有这一套本地存储实现，本阶段遵循“不创建第二套文件存储系统”的要求，没有引入空泛的 `FileStorageService`。
- 项目文件使用 `/profile/private/project/**` 私有目录，安全配置禁止匿名静态读取，只能从鉴权下载接口获得；`storage_path` 不返回前端。
- 后续只有在项目真正接入 MinIO/OSS 时，才从当前清晰的业务调用边界抽取适配接口并迁移对象键，不预先虚构未使用的 provider/bucket 字段。
- ZIP 只保存与受控解压；必须防 Zip Slip、压缩炸弹和嵌套深度攻击。解析任务不能信任文件内的指令文本。
- CSV 需在 CostAI 专用白名单中增加，并同时校验扩展名、MIME、文件签名/可解析性；不要盲目扩大全局上传白名单。

## 七、Excel 能力

### 已有能力

- `ruoyi-common` 已有 POI 5.5.1 和基于 `@Excel` 注解的 `ExcelUtil<T>`。
- 导出使用 `SXSSFWorkbook(500)`，具备流式写出能力。
- 导入使用 `WorkbookFactory.create(InputStream)`，然后构造对象列表；这会打开完整 Workbook，不适合用户要求的 10 万+ 行工程清单。
- 前端已有可复用的 `ExcelImportDialog`，支持 Bearer Token、模板下载、手动确认上传。

### CostAI 方案

- 普通字典、项目列表和审核问题导出继续复用 `ExcelUtil`。
- 阶段 03 未引入 EasyExcel 或升级依赖，直接复用现有 POI 5.5.1：XLSX 用 SAX、XLS 用 HSSF Event、CSV 用流式字符解析。每个 Sheet 只保留前 100 个物理行，接口最多返回 50 条有效预览，5000 行 XLSX 测试验证未整本加载。
- 导入分两段：预览/字段映射（阶段 03，只读少量样本）→ 用户确认 → 全量流式正式导入（阶段 04，已完成）。正式导入复用同一项目文件，XLSX/XLS/CSV 均按行读取，每 500 条使用独立短事务写入，不在文件网络/磁盘读取期间保持数据库长事务。
- 阶段 04 保留 Excel 原始 `total_price`，另存 `calculated_total_price = quantity × unit_price`（6 位小数、`HALF_UP`）供后续审核，不擅自修正原文件金额；失败行进入独立错误表并保留源 Sheet、源行、字段、原值和原因。
- 字段映射先用确定性别名字典和归一化匹配，再在低置信度字段上调用 LLM；用户确认结果固化在导入批次中。
- 解析、校验、批量保存不得处于一个超长事务；以批次状态和错误明细保证可追踪与可重试。

## 八、Redis 能力

`RedisCache` 已支持 value/list/set/hash、过期时间、key 扫描和删除。现有系统已使用 Redis 保存登录会话和字典缓存。

CostAI 可缓存：

- 启用的 AI Provider 路由结果（短 TTL，配置变更主动失效）。
- Prompt 当前启用版本。
- RAG 参数、字典、热点知识库元数据。
- 材料/指标热点查询（第二阶段）。
- SSE 连接的短期状态或任务事件序号（必要时）。

不缓存：完整工程清单、大段解析文本、完整对话历史、审核结果全集、API Key 明文。持久化任务状态以 MySQL 为准，Redis 不能成为唯一事实源。

## 九、可以直接复用的代码

| 能力 | 复用位置 | 使用方式 |
|---|---|---|
| 用户/角色/部门/岗位 | `ruoyi-system` | 负责人、创建人、部门和授权直接关联现有 ID |
| JWT 会话 | `TokenService`、JWT filter | 所有 CostAI API/SSE 复用 Bearer Token |
| RBAC | `PermissionService`、`@PreAuthorize` | 菜单、按钮、后端方法三层一致 |
| 数据权限 | `@DataScope`、`DataScopeAspect` | 项目查询以 `owner_dept_id/project_manager_id` 接入 |
| 菜单路由 | `sys_menu` + permission store | 用 SQL 菜单数据挂载 CostAI 页面 |
| 字典 | `sys_dict_type/data`、`useDict`、`DictTag` | 项目类型、阶段、状态、风险级别等 |
| 操作日志 | `@Log`、`SysOperLog` | 项目 CRUD、导入、审核、报告生成 |
| 登录日志/在线用户 | 现有系统 | 无需重复实现 |
| 统一返回与分页 | `AjaxResult`、`TableDataInfo`、`BaseController` | 普通 REST API 保持若依格式 |
| 异常处理 | `ServiceException`、全局异常处理器 | 新增细分业务异常并转换友好消息 |
| Redis | `RedisCache` | 配置与热点元数据缓存 |
| 文件基础工具 | `FileUploadUtils`、`FileUtils` | 阶段 02 直接复用并限定私有项目目录；未重复建设存储层 |
| Excel 常规导入导出 | `ExcelUtil`、`ExcelImportDialog` | 非大数据场景复用 |
| 图表 | ECharts | 工作台与指标图表 |
| UI 组件 | Element Plus、Pagination、RightToolbar、Upload、Drawer/Tabs | 管理页复用，AI 页组合增强 |
| CRUD 生成器 | `ruoyi-generator` | 可生成骨架，但生成后必须按业务边界调整 |

### 明确需要新增的能力

- CostAI 业务模块、业务表和菜单数据。
- 项目文件元数据、私有路径和项目权限（阶段 02 已完成）。
- BOQ 流式预览、确定性字段映射、导入批次、500 条分块入库、错误行和清单分页管理（阶段 03/04 已完成）。
- 两份清单非 AI 匹配、人工调整和对比页已完成；纯 Java 审核规则、数据库阈值、审核批次/问题和项目审核结果页也已完成。下一步按独立阶段补充问题处理工作流，AI enrichment 必须等待统一 AI 基础层。
- AI Provider 抽象、密钥加密、Prompt 版本、结构化输出、日志和 Token 统计。
- AI 长任务调度与进度事件。
- SSE 客户端/服务端封装、Markdown 安全渲染。
- 知识解析、切片、Embedding、Qdrant 适配、RAG 和来源引用。
- Word/PDF 报告模板生成与记录。
- 单元测试、集成测试和大文件性能测试。

## 十、AI 造价平台建议目录结构

### 后端

为避免八个小 Maven 模块造成依赖和事务边界复杂化，新增一个 `ruoyi-cost` 业务模块，在包内按领域模块化；Controller 继续放 `ruoyi-admin`，符合当前若依工程约定。

```text
ruoyi-cost/
├─ pom.xml
├─ src/main/java/com/ruoyi/cost/
│  ├─ project/       domain, dto, vo, mapper, service
│  ├─ file/          项目文件元数据、现有存储复用、安全校验、解析状态
│  ├─ boq/           导入批次、字段映射、清单、匹配
│  ├─ review/        审核批次、规则引擎、问题工作流
│  ├─ ai/
│  │  ├─ model/      AiModelService 与 Provider Adapter
│  │  ├─ prompt/     Prompt 模板与版本
│  │  ├─ agent/      AiAgent、CostQaAgent、CostReviewAgent、ReportAgent
│  │  ├─ tool/       AiTool 与受控业务工具
│  │  ├─ chat/       会话、消息、来源
│  │  ├─ task/       持久化任务执行器
│  │  └─ log/        请求与 Token 日志
│  ├─ knowledge/     文档、切片、RAG、VectorStoreService/Qdrant
│  ├─ report/        报告数据快照、AI 文字、Word/PDF 模板
│  └─ common/        仅 CostAI 内共享的枚举、计算器、JSON Schema
└─ src/main/resources/mapper/cost/{domain}/

ruoyi-admin/src/main/java/com/ruoyi/web/controller/
├─ cost/             project, file, boq, review, knowledge, report
└─ ai/               chat, provider, prompt, task, log

ruoyi-ui/src/
├─ api/cost/         project.js, file.js, boq.js, review.js, knowledge.js, report.js
├─ api/ai/           chat.js, provider.js, prompt.js, task.js, log.js
├─ views/cost/
│  ├─ dashboard/
│  ├─ project/
│  ├─ boq/
│  ├─ review/
│  ├─ knowledge/
│  └─ report/
├─ views/ai/
│  ├─ assistant/
│  ├─ provider/
│  ├─ prompt/
│  └─ task/
├─ components/cost/  ProjectSelector、RiskTag、BoqTable、SourceCitation
├─ components/ai/    ChatMessage、MarkdownViewer、StreamStatus、ToolCallView
├─ composables/      useAiStream、useTaskProgress
└─ utils/            aiStream.js、costFormat.js
```

### 第一阶段 Agent 范围

只实现：`CostQaAgent`、`KnowledgeAgent`、`BoqMatchAgent`、`CostReviewAgent`、`ReportAgent`。`PriceReviewAgent`、`RateAnalysisAgent`、`IndicatorAnalysisAgent` 仅保留接口位置，第二阶段再开发；CAD/BIM/合同/签证 Agent 不进入 MVP。

### LLM 与 RAG 边界

- `AiModelService` 是唯一模型入口；Controller 和业务 Service 不直接调用 Provider API。
- Provider 第一版优先实现一个 OpenAI Compatible Adapter，可覆盖 OpenAI、DeepSeek、Qwen 兼容接口和 Ollama 兼容模式；Azure/Claude/Gemini 后续独立适配。
- Provider API Key 加密保存，密钥来自环境变量或外部 secret，不提交仓库；列表/详情只返回掩码。
- `VectorStoreService` 隔离向量库，MySQL 部署下第一版采用 Qdrant；业务层不引用 Qdrant SDK 类型。
- 上传文件内容作为 `Retrieved Context`，永远不能拼入 System Prompt。上下文使用明确边界标记，并声明其中指令不可信。
- 确定性金额/数量/重复编码问题由 Java 规则计算；LLM 只做语义匹配、特征矛盾判断、解释和建议。

## 十一、第一阶段数据库设计

按阶段 07 已落地的当前对比、规则审核和问题处理方案，第一阶段规划 21 张业务表，按依赖关系分批建设：

```text
项目主线
cost_project
  ├─ cost_project_file
  ├─ cost_boq_batch ─────────┬─ cost_boq_import_error
  │                         └─ cost_boq_item
  ├─ cost_boq_compare（阶段 05 当前匹配结果）
  ├─ cost_review_rule_config（阶段 06 数据库化规则阈值）
  ├─ cost_review_task ── cost_review_issue（阶段 06/07 规则结果与人工处理）
  └─ cost_report

AI 主线
ai_model_config（阶段 08 已实现）
ai_prompt_template（每行一个版本，阶段 08 已实现）
ai_conversation ── ai_message（来源与工具摘要使用JSON元数据）
ai_task
ai_request_log（阶段 08 已实现）

知识主线
knowledge_base ── knowledge_document ── knowledge_chunk
```

关键原则：

- 所有可变业务表使用 `bigint auto_increment` 主键、`create_by/create_time/update_by/update_time/remark/del_flag`。
- 日志和消息正文即使不做常规更新，也保留统一审计字段以符合现有规范。
- `cost_project.owner_dept_id` 和 `project_manager_id` 是项目数据权限必需字段。
- 每次 BOQ 导入、审核和报告生成保留独立批次/版本；阶段 05 对比表维护现行结果，审核任务保存 `JAVA_RULES_V1` 和完整配置快照，后续修改阈值不篡改历史证据。
- 清单原值和对比结果用结构化 decimal 字段，不依赖 JSON 计算。
- AI 输出可以存 JSON，但任何金额统计先由 Java 计算并以快照输入模型。
- `knowledge_chunk` 保存文本和向量库引用；向量本体进入 Qdrant，不写 MySQL blob。
- Provider 密钥只保存密文、IV/版本和掩码提示，不返回密文到前端。

字段、索引、状态和迁移顺序见 `AI_COST_DATABASE.md`。

## 十二、第一阶段开发任务拆分

严格按“分析 → 设计 → 实现 → 后端编译 → 前端构建 → 测试 → 修复 → 文档更新”推进。每个任务开始前先说明拟修改/新增文件、数据库变更和影响面。

### P0：工程基线与共享约定

1. 修正 Maven 的 JDK 17 环境并完成前后端基线构建。
2. 建立 Git 基线（需用户确认仓库来源/是否缺失 `.git`）。
3. 新增 `ruoyi-cost` 模块、测试依赖、业务异常与枚举约定。
4. 创建第一批迁移 SQL：项目、文件、字典、菜单和权限。

### P1：项目与文件

1. 项目 CRUD、数据权限、字典、菜单、前端列表与详情 Tabs。（阶段 01 已完成）
2. 项目文件上传/列表/详情/分类/下载/删除/解析状态，直接复用现有本地存储。（阶段 02 已完成）
3. 文件安全校验已完成；异步解析任务在任务中心阶段接入，本阶段只保留真实状态和重新解析按钮占位。

### P2：BOQ 导入与管理

1. 表头采样、Sheet 识别、字段别名规则、有限预览和用户确认三步页。（阶段 03 已完成）
2. 导入批次、用户确认映射持久化和正式全量流式解析。（阶段 04 已完成）
3. 每 500 条批量入库、错误明细、批次/清单分页管理页。（阶段 04 已完成）
4. AI 低置信度字段映射仅作为后续增强，不作为阶段 03/04 的强依赖。
5. BigDecimal 原值/计算值、500 条分块与 10 万行 CSV 流式读取验证。（阶段 04 已完成）

### P3：两表对比与审核

1. 编码、标准化名称/单位、名称/特征相似度多阶段一对一匹配及人工调整。（阶段 05 已完成）
2. 当前对比结果表、BigDecimal 左右差异、项目详情对比页和批次删除联动。（阶段 05 已完成）
3. Java 规则：数量/单价负数、零单价、合价计算、重复、工程量/单价/合价差异、仅左/仅右。（阶段 06 已完成）
4. 数据库化启停/阈值/容差、规则版本与配置快照、审核批次/问题持久化和项目审核结果页。（阶段 06 已完成）
5. 审核任务、问题详情、确认/忽略/整改状态机、人工意见和统计回算。（阶段 07 已完成）
6. 补充单位缺失、编码格式等确定性规则。（后续规则扩展）
7. `CostReviewAgent` 对候选异常做单问题语义判断、解释和建议；只发送左右各一条清单，结果必须人工确认。（阶段 09 已完成）

### P4：AI 基础层与聊天

1. 模型配置管理、AES-256-GCM 密钥加密、默认路由与 OpenAI Compatible Adapter。（阶段 08 已完成）
2. Prompt 模板同表版本管理与启停；发布工作流和缓存后续增强。（阶段 08 基础能力已完成）
3. AI 请求日志、Token/时延/错误审计。（阶段 08 已完成）
4. 通用 `AiAgent<C,R>` 契约、严格 JSON Schema 输出、Prompt Injection 数据边界和短事务建议落库。（阶段 09 已由 CostReviewAgent 首次落地）
5. 当前用户隔离的会话/消息、项目有限上下文、受控只读工具、SSE流与安全Markdown页面。（阶段 10 已完成）

### P5：知识库与 RAG

1. 知识库/文档管理、PDF/DOCX/TXT解析、扫描PDF `OCR_REQUIRED` 已完成（阶段11）。
2. 标题/段落/页码语义分片、Qdrant适配、Embedding索引与带来源RAG已完成（阶段11）。
3. 复用项目文件与数据权限、知识中心管理/问答页面已完成；Excel知识解析、OCR、rerank及专用`KnowledgeAgent`按后续独立阶段评测后引入。
2. 切片、Token 上限、Embedding、Qdrant 适配。
3. TopK/阈值/上下文预算、引用来源和 Prompt Injection 防护。
4. 知识问答和项目问答工具编排。

### P6：报告与工作台

1. Java 统计快照 + AI 文字 + Word 模板 + PDF 转换策略。
2. 报告预览、版本、导出和错误恢复。
3. 工作台真实聚合查询和 ECharts 图表，禁止 Mock 数据。

### MVP 验收门槛

- 典型两份造价 Excel 可经字段确认后完整导入、匹配、审核并生成问题及报告。
- 金额计算无 double；核心测试覆盖空值、0、负数、舍入、超大金额。
- 所有接口有 RBAC，项目列表与详情均验证数据权限。
- AI 网络调用不占用数据库长事务；所有长任务可查进度、失败原因和重试边界。
- AI 回答以结构化内容返回并显示可追溯来源；无来源时明确说明。
- Provider Key 不明文存储、不返回前端、不写日志。
- 后端 `BUILD SUCCESS`、前端生产构建成功、核心测试通过后才进入下一模块。
