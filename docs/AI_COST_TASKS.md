# CostAI 开发任务清单

> 更新日期：2026-09-01  
> 当前里程碑：阶段 01 造价项目管理完成；未进入项目文件阶段。  
> 规则：每完成一项立即更新状态、验证结果和相关文档；不得用“代码已写”代替“已编译并测试”。

## 状态说明

- `[ ]` 未开始
- `[~]` 进行中
- `[x]` 已完成并验证
- `[!]` 阻塞，必须在备注写明原因

## 0. 项目扫描与基线

- [x] 扫描 Maven 模块、版本和依赖关系
- [x] 分析 Controller / Service / Mapper / Entity / 分页规范
- [x] 分析 Spring Security、JWT、RBAC、菜单和数据权限
- [x] 分析本地文件上传能力与限制
- [x] 分析 POI Excel 导入导出能力与大文件限制
- [x] 分析 Redis、异常、日志、SpringDoc、异步/Quartz
- [x] 分析 Vue 版本、JavaScript、Pinia、Router、请求和权限指令
- [x] 确认 Markdown、SSE/WebSocket、对象存储、向量库尚未实现
- [x] 创建 `AI_COST_PLATFORM_PLAN.md`
- [x] 创建 `AI_COST_DATABASE.md`
- [x] 创建 `AI_COST_API.md`
- [x] 创建 `AI_COST_TASKS.md`
- [!] Git diff/最近提交检查：当前目录不是 Git 仓库，无 `.git`
- [!] 默认 Maven 环境：系统 `JAVA_HOME` 仍指向 JDK 8，工程要求 JDK 17
- [x] 临时指定 JDK 17 完成后端基线 `mvn -DskipTests package`，7 个模块 `BUILD SUCCESS`
- [x] 临时指定 JDK 17 完成 `mvn test`，`BUILD SUCCESS`；当前没有测试源码
- [x] 完成前端基线 `npm run build:prod`，Vite 构建成功
- [ ] 确认仓库来源并建立/恢复 Git 基线

## 1. 工程骨架与公共约定（P0）

- [x] 开发前声明本任务修改文件、数据库变更和影响面
- [x] 新增 `ruoyi-cost` Maven 模块并接入父 POM/`ruoyi-admin`
- [x] 建立 `com.ruoyi.cost` 包内领域结构
- [~] 建立 CostAI 枚举、业务异常、金额计算和 JSON 配置约定（项目状态与金额计算已建立，AI JSON 约定留待对应阶段）
- [x] 配置 CostAI SpringDoc 分组并补充项目接口说明
- [x] 增加后端单元测试基础依赖与 Surefire/JUnit 5 测试规范
- [x] 确定 SQL 版本脚本命名：`业务_YYYYMMDD.sql`，阶段脚本采用可重复执行的字典/菜单写入
- [x] 后端编译成功
- [x] 前端构建成功
- [x] 更新四份文档

## 2. 项目管理（P1-1）

- [x] `cost_project` 数据库与四类字典 SQL
- [x] 项目 Entity/DTO/VO/Mapper/Service/Controller
- [x] 接入 `@DataScope`：本人/本部门/子部门/全部
- [x] 接入 `@PreAuthorize` 与 `@Log`
- [x] 项目列表、表单、统计卡片、详情 Tabs
- [x] 菜单与按钮权限 SQL
- [x] 项目编号重复校验与数据库唯一键
- [x] 项目金额/核减率 BigDecimal 测试（空值、0、负分母、舍入、超大金额）
- [x] 逻辑删除与加权统计测试
- [x] 数据权限注解/SQL 字段契约测试及“仅本人”AOP 注入测试
- [x] Controller 权限注解与分页返回契约测试
- [x] 阶段 SQL 已在本地 `ry-vue`（MySQL 8.0.11）执行：1 张表、4 类字典/33 个字典项、6 个菜单/按钮
- [x] 后端编译、前端构建、测试通过
- [x] 应用启动成功并完成真实 HTTP 冒烟：登录、新增、重复编号、分页、详情、状态、统计、逻辑删除；测试项目已精确清理
- [x] 更新数据库/API/任务文档

### 阶段 01 验证记录（2026-09-01）

- 后端：JDK 17 执行 `mvn package`，8 个 Reactor 模块全部 `BUILD SUCCESS`。
- 测试：`ruoyi-cost` 10 项、`ruoyi-admin` 2 项，共 12 项，0 失败、0 错误。
- 前端：`npm run build:prod`，Vite 生产构建成功（2547 modules transformed）。
- 数据库：本地 MySQL 8.0.11 连续执行迁移两次，均成功且数量保持 `cost_project=1` 张表、字典类型 4、字典项 33、菜单/按钮 6。
- HTTP：随机编号项目的新增/重复拦截/分页/详情/状态/统计/删除均通过；核减率与加权核减率均返回 `0.125`。
- 清理：冒烟项目已按随机项目 ID + 编号物理清理，未向项目表保留测试/模拟数据。
- Git：工作目录仍无 `.git`，无法生成可信的 `git status/diff/最近提交` 报告。

## 3. 项目文件（P1-2）

- [ ] `cost_project_file` 表
- [ ] `FileStorageService` 接口
- [ ] 本地存储适配器（复用 `FileUploadUtils`）
- [ ] 项目文件专用上传/下载/删除接口
- [ ] PDF/Word/Excel/CSV/TXT/图片/ZIP 安全白名单
- [ ] 文件哈希、元数据和解析状态
- [ ] ZIP Slip/压缩炸弹防护
- [ ] 项目详情“项目文件”Tab
- [ ] 文件权限与异常测试
- [ ] 后端编译、前端构建、测试通过
- [ ] 更新文档

## 4. Excel 字段映射与 BOQ 导入（P2）

- [ ] `cost_boq_import_batch` / `cost_boq_import_error` / `cost_boq_item` 表
- [ ] 表头行和 Sheet 识别
- [ ] 字段别名与归一化规则匹配
- [ ] AI 低置信度字段映射
- [ ] 用户确认映射接口与页面
- [ ] 技术验证 EasyExcel 与 POI 5.5.1 兼容性
- [ ] 流式读取与 500~1000 条批量写入
- [ ] 导入取消、失败、部分失败和错误文件
- [ ] 清单列表/详情/人工修正/导出
- [ ] 10 万行内存和耗时验证
- [ ] 空值、公式、合并单元格、科学计数法、超大金额测试
- [ ] 后端编译、前端构建、测试通过
- [ ] 更新文档

## 5. 两份清单匹配（P3-1）

- [ ] `cost_boq_compare_batch` / `cost_boq_compare_result` 表
- [ ] 项目编码精确匹配
- [ ] 名称/单位/特征归一化匹配
- [ ] 字符串相似度候选召回
- [ ] Embedding 候选匹配（在 AI 基础层可用后）
- [ ] LLM 歧义判断（仅候选项）
- [ ] 漏项/新增/歧义分类
- [ ] 人工调整匹配与重跑版本
- [ ] 匹配准确率测试集
- [ ] 后端编译、前端构建、测试通过
- [ ] 更新文档

## 6. 规则审核与问题管理（P3-2）

- [ ] `cost_review_batch` / `cost_review_issue` 表
- [ ] 工程量差异规则
- [ ] 综合单价差异规则
- [ ] 数量 × 单价与合价校验
- [ ] 负数、零单价、单位缺失、编码异常规则
- [ ] 重复清单规则
- [ ] 漏项/新增问题生成
- [ ] 风险金额和风险级别计算
- [ ] `CostReviewAgent` 语义分析与建议 enrichment
- [ ] 问题确认/忽略/整改工作流
- [ ] 审核主页面与问题 Drawer
- [ ] BigDecimal 舍入、0 分母、负数、超大金额测试
- [ ] 后端编译、前端构建、测试通过
- [ ] 更新文档

## 7. AI Provider、Prompt 与日志（P4-1）

- [ ] `ai_provider` 表与密钥加密方案
- [ ] `AiModelService` 统一接口
- [ ] OpenAI Compatible Provider Adapter
- [ ] Provider 路由、超时、重试、熔断边界
- [ ] Provider 管理页和连接测试
- [ ] `ai_prompt_template` / `ai_prompt_version` 表
- [ ] Prompt 草稿、发布、回滚/停用和缓存
- [ ] Structured Output JSON Schema 校验与失败处理
- [ ] `ai_request_log` 与 Token/耗时统计
- [ ] Key 不回显、不入日志测试
- [ ] 后端编译、前端构建、测试通过
- [ ] 更新文档

## 8. AI 任务中心（P4-2）

- [ ] `ai_task` 表
- [ ] 持久化 Worker 领取、心跳/超时和幂等
- [ ] 进度、成功数、失败数与结果摘要
- [ ] 安全取消和失败重试
- [ ] 任务列表/详情页面
- [ ] SSE 任务进度与断线后的查询恢复
- [ ] 多 Worker 防重复执行测试
- [ ] 后端编译、前端构建、测试通过
- [ ] 更新文档

## 9. AI 造价聊天（P4-3）

- [ ] `ai_chat_session` / `ai_chat_message` / `ai_message_source` 表
- [ ] 会话列表、项目/知识库上下文和消息分页
- [ ] 后端 `SseEmitter` 生命周期与心跳
- [ ] 前端带 Authorization 的 fetch 流封装
- [ ] 结构化回答：answer/sources/suggestions/toolCalls/tokenUsage
- [ ] Markdown 渲染、链接和 HTML XSS 清洗
- [ ] 左会话/中聊天/右上下文与工具布局
- [ ] 停止生成、失败提示、断线恢复
- [ ] Prompt Injection 边界测试
- [ ] 后端编译、前端构建、测试通过
- [ ] 更新文档

## 10. 知识库与 RAG（P5）

- [ ] `knowledge_base` / `knowledge_document` / `knowledge_chunk` 表
- [ ] `VectorStoreService` 接口
- [ ] Qdrant Adapter 和 collection 命名/维度策略
- [ ] PDF/Word/TXT 解析技术验证
- [ ] 分片、页码/章节、Token 预算和内容哈希
- [ ] Embedding 入库、删除和重建索引
- [ ] TopK、阈值、最大上下文和可选 rerank
- [ ] 来源引用与原文定位
- [ ] `KnowledgeAgent` / `CostQaAgent`
- [ ] 知识库管理与文档状态页面
- [ ] 检索质量、隔离、删除一致性测试
- [ ] 后端编译、前端构建、测试通过
- [ ] 更新文档

## 11. 报告中心（P6-1）

- [ ] `cost_report` 表
- [ ] 审核统计快照（Java 计算）
- [ ] 报告 AI 文字 Structured Output
- [ ] Word 模板技术选型/验证（优先 poi-tl，检查 POI 兼容性）
- [ ] PDF 转换策略与部署依赖说明
- [ ] 报告版本、预览、人工修改和下载
- [ ] 报告数字一致性测试
- [ ] 后端编译、前端构建、测试通过
- [ ] 更新文档

## 12. 工作台（P6-2）

- [ ] 项目/金额/核减率真实聚合
- [ ] AI 次数/问题/Token/任务真实聚合
- [ ] 最近 12 月金额趋势
- [ ] 问题类型和风险统计
- [ ] 最近项目列表
- [ ] 应用项目数据权限
- [ ] ECharts 响应式与空数据状态
- [ ] 查询性能与索引验证
- [ ] 后端编译、前端构建、测试通过
- [ ] 更新文档

## 13. 第一阶段暂不开发

- [ ] 材料价格中心（第二阶段）
- [ ] 历史造价指标库（第二阶段）
- [ ] 智能组价（第二阶段）
- [ ] CAD/DWG/DXF 解析（第三阶段）
- [ ] BIM/IFC 算量（第三阶段）
- [ ] 合同、变更签证、索赔、结算 Agent（第三阶段）

## 阶段 0 发现的待决策/风险

1. 当前目录缺少 Git 元数据，无法按长期规则读取 diff 和最近提交。
2. 默认 Maven 使用 JDK 8 而项目要求 JDK 17；阶段 0 已用临时 JDK 17 验证构建成功，但默认环境仍需修正。
3. 文件限制分散在 Spring 10 MB、工具 50 MB、前端组件 5 MB 三层，项目文件模块需统一策略。
4. 现有上传只支持本地文件系统且没有文件业务元数据；需要存储抽象，但第一版仍复用本地实现。
5. 现有 Excel 导入为整 Workbook 打开，不满足 10 万行；必须先做流式方案兼容性验证。
6. 当前 SpringDoc 配置只扫描 tool Controller；CostAI API 需独立分组。
7. 当前通用异常处理对未知异常返回 `e.getMessage()`，CostAI 业务异常必须主动转换为安全友好消息，避免泄露 Provider/文件系统细节。
8. 前端无 Markdown/SSE；新增依赖必须小范围、可审计，不做全量 TypeScript 迁移。
