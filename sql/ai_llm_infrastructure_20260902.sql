-- CostAI 阶段08：统一 LLM 基础设施
-- 可重复执行；新增模型配置、Prompt 模板、AI 请求日志及系统管理菜单权限。

create table if not exists ai_model_config (
    id                  bigint not null auto_increment comment '模型配置ID',
    name                varchar(100) not null comment '配置名称',
    provider_type       varchar(32) not null comment '提供商类型',
    base_url            varchar(500) not null comment 'OpenAI Compatible API 基础地址',
    api_key_encrypted   longtext null comment 'AES-GCM 加密后的 API Key',
    api_key_hint        varchar(64) null comment 'API Key 脱敏提示',
    chat_model          varchar(100) not null comment '对话模型',
    embedding_model     varchar(100) null comment 'Embedding 模型',
    temperature         decimal(4,3) not null default 0.200 comment '默认温度',
    max_tokens          int not null default 4096 comment '默认最大输出 Token',
    timeout_seconds     int not null default 60 comment '请求超时秒数',
    enabled             char(1) not null default '1' comment '状态（0启用 1停用）',
    is_default          char(1) not null default 'N' comment '是否默认（Y是 N否）',
    create_by           varchar(64) default '' comment '创建者',
    create_time         datetime not null default current_timestamp comment '创建时间',
    update_by           varchar(64) default '' comment '更新者',
    update_time         datetime null comment '更新时间',
    remark              varchar(500) null comment '备注',
    del_flag            char(1) not null default '0' comment '删除标志（0存在 2删除）',
    active_name         varchar(100) generated always as
                            (if(del_flag = '0', name, null)) stored comment '有效配置唯一键',
    primary key (id),
    unique key uk_ai_model_config_active_name (active_name),
    key idx_ai_model_config_route (enabled, is_default, id),
    key idx_ai_model_config_provider (provider_type, enabled)
) engine=InnoDB default charset=utf8mb4 comment='AI 模型配置';

create table if not exists ai_prompt_template (
    id                  bigint not null auto_increment comment 'Prompt 模板ID',
    prompt_code         varchar(64) not null comment 'Prompt 编码',
    prompt_name         varchar(100) not null comment 'Prompt 名称',
    system_prompt       longtext not null comment '系统 Prompt',
    user_template       longtext not null comment '用户 Prompt 模板',
    version             int not null default 1 comment '版本号',
    enabled             char(1) not null default '1' comment '状态（0启用 1停用）',
    create_by           varchar(64) default '' comment '创建者',
    create_time         datetime not null default current_timestamp comment '创建时间',
    update_by           varchar(64) default '' comment '更新者',
    update_time         datetime null comment '更新时间',
    remark              varchar(500) null comment '备注',
    del_flag            char(1) not null default '0' comment '删除标志（0存在 2删除）',
    active_version_key  varchar(80) generated always as
                            (if(del_flag = '0', concat(prompt_code, '#', version), null)) stored
                            comment '有效Prompt版本唯一键',
    primary key (id),
    unique key uk_ai_prompt_active_version (active_version_key),
    key idx_ai_prompt_active (prompt_code, enabled, version),
    key idx_ai_prompt_name (prompt_name)
) engine=InnoDB default charset=utf8mb4 comment='AI Prompt 模板版本';

create table if not exists ai_request_log (
    id                  bigint not null auto_increment comment '请求日志ID',
    user_id             bigint null comment '调用用户ID',
    model_config_id     bigint null comment '模型配置ID',
    provider_type       varchar(32) null comment '提供商类型快照',
    model_name          varchar(100) null comment '模型名称快照',
    request_type        varchar(32) not null comment 'CHAT/STREAM_CHAT/STRUCTURED_CHAT/EMBEDDING/CONNECTION_TEST',
    business_type       varchar(64) null comment '业务类型',
    business_id         varchar(100) null comment '业务ID',
    request_id          varchar(100) null comment '提供商请求ID',
    prompt_tokens       int not null default 0 comment '输入 Token',
    completion_tokens   int not null default 0 comment '输出 Token',
    total_tokens        int not null default 0 comment '总 Token',
    duration_ms         bigint not null default 0 comment '耗时毫秒',
    success             char(1) not null comment '是否成功（Y是 N否）',
    error_code          varchar(64) null comment '错误码',
    error_message       varchar(1000) null comment '脱敏错误信息',
    create_by           varchar(64) default '' comment '创建者',
    create_time         datetime not null default current_timestamp comment '创建时间',
    update_by           varchar(64) default '' comment '更新者',
    update_time         datetime null comment '更新时间',
    remark              varchar(500) null comment '备注',
    del_flag            char(1) not null default '0' comment '删除标志（0存在 2删除）',
    primary key (id),
    key idx_ai_request_log_user (user_id, create_time),
    key idx_ai_request_log_model (model_config_id, create_time),
    key idx_ai_request_log_business (business_type, business_id),
    key idx_ai_request_log_success (success, create_time),
    key idx_ai_request_log_type (request_type, create_time)
) engine=InnoDB default charset=utf8mb4 comment='AI 模型调用日志';

insert into sys_dict_type(dict_name, dict_type, status, create_by, create_time, remark)
select 'AI提供商类型', 'ai_provider_type', '0', 'admin', sysdate(), 'AI 模型提供商协议类型'
where not exists (select 1 from sys_dict_type where dict_type = 'ai_provider_type');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
     status, create_by, create_time, remark)
select 1, 'OpenAI Compatible', 'OPENAI_COMPATIBLE', 'ai_provider_type', '', 'primary',
       'Y', '0', 'admin', sysdate(), '兼容 /chat/completions 与 /embeddings'
where not exists (select 1 from sys_dict_data
    where dict_type = 'ai_provider_type' and dict_value = 'OPENAI_COMPATIBLE');

set @system_menu_id = (
    select menu_id from sys_menu where parent_id = 0 and path = 'system' and menu_type = 'M' limit 1
);

insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'AI模型配置', @system_menu_id, 7, 'ai-model', 'ai/model/index', '', 'AiModelConfig',
       1, 0, 'C', '0', '0', 'ai:model:list', 'connection', 'admin', sysdate(), '统一 AI 模型配置'
where @system_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'ai:model:list' and menu_type = 'C');
set @ai_model_menu_id = (select menu_id from sys_menu where perms = 'ai:model:list' and menu_type = 'C' limit 1);

insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select v.menu_name, @ai_model_menu_id, v.order_num, '', null, '', '', 1, 0,
       'F', '0', '0', v.perms, '#', 'admin', sysdate(), v.remark
from (
    select '模型查询' menu_name, 1 order_num, 'ai:model:query' perms, '查询模型配置' remark union all
    select '模型新增', 2, 'ai:model:add', '新增模型配置' union all
    select '模型修改', 3, 'ai:model:edit', '修改模型配置' union all
    select '模型删除', 4, 'ai:model:remove', '删除模型配置' union all
    select '连接测试', 5, 'ai:model:test', '测试模型连接'
) v
where @ai_model_menu_id is not null
  and not exists (select 1 from sys_menu m where m.perms = v.perms);

insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'Prompt模板', @system_menu_id, 8, 'ai-prompt', 'ai/prompt/index', '', 'AiPromptTemplate',
       1, 0, 'C', '0', '0', 'ai:prompt:list', 'edit', 'admin', sysdate(), 'AI Prompt 模板版本管理'
where @system_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'ai:prompt:list' and menu_type = 'C');
set @ai_prompt_menu_id = (select menu_id from sys_menu where perms = 'ai:prompt:list' and menu_type = 'C' limit 1);

insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select v.menu_name, @ai_prompt_menu_id, v.order_num, '', null, '', '', 1, 0,
       'F', '0', '0', v.perms, '#', 'admin', sysdate(), v.remark
from (
    select 'Prompt查询' menu_name, 1 order_num, 'ai:prompt:query' perms, '查询 Prompt 模板' remark union all
    select 'Prompt新增', 2, 'ai:prompt:add', '新增 Prompt 模板版本' union all
    select 'Prompt修改', 3, 'ai:prompt:edit', '修改 Prompt 模板版本' union all
    select 'Prompt删除', 4, 'ai:prompt:remove', '删除 Prompt 模板版本'
) v
where @ai_prompt_menu_id is not null
  and not exists (select 1 from sys_menu m where m.perms = v.perms);

insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'AI调用日志', @system_menu_id, 9, 'ai-log', 'ai/log/index', '', 'AiRequestLog',
       1, 0, 'C', '0', '0', 'ai:log:list', 'log', 'admin', sysdate(), '脱敏 AI 调用日志'
where @system_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'ai:log:list' and menu_type = 'C');
set @ai_log_menu_id = (select menu_id from sys_menu where perms = 'ai:log:list' and menu_type = 'C' limit 1);

insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '日志查询', @ai_log_menu_id, 1, '', null, '', '', 1, 0,
       'F', '0', '0', 'ai:log:query', '#', 'admin', sysdate(), '查询 AI 调用日志详情'
where @ai_log_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'ai:log:query');
