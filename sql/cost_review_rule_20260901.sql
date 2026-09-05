-- CostAI 阶段06：纯Java造价审核规则、审核批次、问题、字典与权限
-- 可重复执行；不覆盖管理员已调整的规则配置值。

create table if not exists cost_review_rule_config (
    id             bigint        not null auto_increment comment '规则配置ID',
    rule_code      varchar(64)   not null comment '规则编码',
    rule_name      varchar(100)  not null comment '规则名称',
    config_key     varchar(64)   not null comment '配置键',
    config_name    varchar(100)  not null comment '配置名称',
    config_value   varchar(1000) not null comment '配置值',
    value_type     varchar(32)   not null comment 'BOOLEAN/DECIMAL/STRING_LIST',
    sort_num       int           not null default 0 comment '显示顺序',
    description    varchar(500)  default null comment '配置说明',
    create_by      varchar(64)   default '' comment '创建者',
    create_time    datetime      not null default current_timestamp comment '创建时间',
    update_by      varchar(64)   default '' comment '更新者',
    update_time    datetime      default null comment '更新时间',
    remark         varchar(500)  default null comment '备注',
    del_flag       char(1)       not null default '0' comment '删除标志（0存在 2删除）',
    primary key (id),
    unique key uk_review_rule_config (rule_code, config_key),
    key idx_review_rule_config_sort (rule_code, sort_num)
) engine=InnoDB comment='造价审核规则配置';

create table if not exists cost_review_batch (
    id                   bigint         not null auto_increment comment '审核批次ID',
    project_id           bigint         not null comment '项目ID',
    left_batch_id        bigint         not null comment '左侧清单批次ID',
    right_batch_id       bigint         not null comment '右侧清单批次ID',
    status               varchar(32)    not null comment '审核状态',
    rule_version         varchar(32)    not null comment '规则引擎版本',
    config_snapshot_json longtext       not null comment '本次规则配置快照',
    left_item_count      int            not null default 0 comment '左侧清单数',
    right_item_count     int            not null default 0 comment '右侧清单数',
    compare_count        int            not null default 0 comment '对比结果数',
    issue_count          int            not null default 0 comment '问题总数',
    medium_count         int            not null default 0 comment '中风险问题数',
    high_count           int            not null default 0 comment '高风险问题数',
    critical_count       int            not null default 0 comment '严重问题数',
    risk_amount          decimal(24,6)  not null default 0 comment '问题风险金额汇总',
    started_by           varchar(64)    not null comment '执行人',
    start_time           datetime       not null comment '开始时间',
    finish_time          datetime       default null comment '完成时间',
    error_message        varchar(1000)  default null comment '失败原因',
    create_by            varchar(64)    default '' comment '创建者',
    create_time          datetime       not null default current_timestamp comment '创建时间',
    update_by            varchar(64)    default '' comment '更新者',
    update_time          datetime       default null comment '更新时间',
    remark               varchar(500)   default null comment '备注',
    del_flag             char(1)        not null default '0' comment '删除标志（0存在 2删除）',
    primary key (id),
    key idx_review_batch_project (project_id, create_time),
    key idx_review_batch_pair (project_id, left_batch_id, right_batch_id),
    key idx_review_batch_left (left_batch_id),
    key idx_review_batch_right (right_batch_id),
    key idx_review_batch_status (project_id, status)
) engine=InnoDB comment='造价规则审核批次';

create table if not exists cost_review_issue (
    id                   bigint         not null auto_increment comment '审核问题ID',
    review_batch_id      bigint         not null comment '审核批次ID',
    project_id           bigint         not null comment '项目ID',
    boq_item_id          bigint         default null comment '主要关联清单ID',
    compare_result_id    bigint         default null comment '关联对比结果ID',
    item_side            varchar(16)    default null comment 'LEFT/RIGHT/BOTH',
    item_code_snapshot   varchar(100)   default null comment '清单编码快照',
    item_name_snapshot   varchar(500)   default null comment '清单名称快照',
    issue_type           varchar(64)    not null comment '问题类型',
    issue_level          varchar(16)    not null comment '问题级别',
    issue_title          varchar(200)   not null comment '问题标题',
    issue_description    varchar(2000)  not null comment '问题说明',
    original_value       varchar(1000)  default null comment '原值',
    reference_value      varchar(1000)  default null comment '参考值',
    difference_value     decimal(24,8)  default null comment '差异值',
    difference_rate      decimal(18,6)  default null comment '差异率',
    risk_amount          decimal(24,6)  not null default 0 comment '风险金额',
    rule_code            varchar(64)    not null comment '命中规则编码',
    evidence_json        longtext       default null comment '可追溯规则证据',
    ai_analysis          longtext       default null comment 'AI分析，阶段06不写入',
    ai_suggestion        longtext       default null comment 'AI建议，阶段06不写入',
    ai_confidence        decimal(8,6)   default null comment 'AI置信度，阶段06不写入',
    status               varchar(32)    not null default 'PENDING' comment '问题状态',
    reviewer_user_id     bigint         default null comment '处理人用户ID',
    review_comment       varchar(2000)  default null comment '处理意见',
    review_time          datetime       default null comment '处理时间',
    create_by            varchar(64)    default '' comment '创建者',
    create_time          datetime       not null default current_timestamp comment '创建时间',
    update_by            varchar(64)    default '' comment '更新者',
    update_time          datetime       default null comment '更新时间',
    remark               varchar(500)   default null comment '备注',
    del_flag             char(1)        not null default '0' comment '删除标志（0存在 2删除）',
    primary key (id),
    key idx_review_issue_batch (review_batch_id, issue_level, issue_type),
    key idx_review_issue_project (project_id, status),
    key idx_review_issue_boq (boq_item_id),
    key idx_review_issue_compare (compare_result_id),
    key idx_review_issue_rule (review_batch_id, rule_code)
) engine=InnoDB comment='造价规则审核问题';

insert into cost_review_rule_config
    (rule_code, rule_name, config_key, config_name, config_value, value_type,
     sort_num, description, create_by, create_time)
select v.rule_code, v.rule_name, v.config_key, v.config_name, v.config_value,
       v.value_type, v.sort_num, v.description, 'admin', sysdate()
from (
    select 'NEGATIVE_QUANTITY' rule_code, '数量异常' rule_name, 'enabled' config_key, '是否启用' config_name, 'true' config_value, 'BOOLEAN' value_type, 1 sort_num, '检测工程量小于0' description union all
    select 'NEGATIVE_UNIT_PRICE', '单价异常', 'enabled', '是否启用', 'true', 'BOOLEAN', 1, '检测综合单价小于0' union all
    select 'ZERO_UNIT_PRICE', '零单价', 'enabled', '是否启用', 'true', 'BOOLEAN', 1, '检测有工程量但综合单价为0' union all
    select 'ZERO_UNIT_PRICE', '零单价', 'exemptKeywords', '豁免关键词', '暂估价,暂列金额,专业工程暂估价,计日工,总承包服务费', 'STRING_LIST', 2, '项目名称或特征包含任一关键词时不报零单价' union all
    select 'TOTAL_CALC_ERROR', '合价计算异常', 'enabled', '是否启用', 'true', 'BOOLEAN', 1, '检测工程量乘综合单价与Excel合价偏差' union all
    select 'TOTAL_CALC_ERROR', '合价计算异常', 'absoluteTolerance', '绝对容差', '0.05', 'DECIMAL', 2, '金额绝对偏差不超过该值时忽略' union all
    select 'TOTAL_CALC_ERROR', '合价计算异常', 'relativeTolerance', '相对容差', '0.001', 'DECIMAL', 3, '相对偏差不超过该值时忽略，0.001表示0.1%' union all
    select 'TOTAL_CALC_ERROR', '合价计算异常', 'highRate', '高风险阈值', '0.05', 'DECIMAL', 4, '相对偏差达到该值时标记高风险' union all
    select 'DUPLICATE_ITEM', '重复清单', 'enabled', '是否启用', 'true', 'BOOLEAN', 1, '按标准化编码、名称、单位、特征检测同批次重复项' union all
    select 'QUANTITY_DIFF', '工程量差异', 'enabled', '是否启用', 'true', 'BOOLEAN', 1, '检测左右清单工程量差异' union all
    select 'QUANTITY_DIFF', '工程量差异', 'warningRate', '预警阈值', '0.10', 'DECIMAL', 2, '差异率达到该值生成问题' union all
    select 'QUANTITY_DIFF', '工程量差异', 'highRate', '高风险阈值', '0.30', 'DECIMAL', 3, '差异率达到该值标记高风险' union all
    select 'UNIT_PRICE_DIFF', '综合单价差异', 'enabled', '是否启用', 'true', 'BOOLEAN', 1, '检测左右清单综合单价差异' union all
    select 'UNIT_PRICE_DIFF', '综合单价差异', 'warningRate', '预警阈值', '0.10', 'DECIMAL', 2, '差异率达到该值生成问题' union all
    select 'UNIT_PRICE_DIFF', '综合单价差异', 'highRate', '高风险阈值', '0.20', 'DECIMAL', 3, '差异率达到该值标记高风险' union all
    select 'TOTAL_PRICE_DIFF', '合价差异', 'enabled', '是否启用', 'true', 'BOOLEAN', 1, '检测左右清单合价差异' union all
    select 'TOTAL_PRICE_DIFF', '合价差异', 'warningRate', '预警阈值', '0.10', 'DECIMAL', 2, '差异率达到该值生成问题' union all
    select 'TOTAL_PRICE_DIFF', '合价差异', 'highRate', '高风险阈值', '0.20', 'DECIMAL', 3, '差异率达到该值标记高风险' union all
    select 'ONLY_LEFT', '仅左侧存在', 'enabled', '是否启用', 'true', 'BOOLEAN', 1, '检测疑似删除项或右侧漏项' union all
    select 'ONLY_RIGHT', '仅右侧存在', 'enabled', '是否启用', 'true', 'BOOLEAN', 1, '检测疑似新增项或左侧漏项'
) v
where not exists (
    select 1 from cost_review_rule_config c
    where c.rule_code = v.rule_code and c.config_key = v.config_key
);

insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select v.dict_name, v.dict_type, '0', 'admin', sysdate(), v.remark
from (
    select '规则审核状态' dict_name, 'cost_review_status' dict_type, 'CostAI规则审核批次状态' remark union all
    select '审核问题类型', 'cost_review_issue_type', 'CostAI确定性审核问题类型' union all
    select '审核问题级别', 'cost_issue_level', 'CostAI统一风险级别' union all
    select '审核问题状态', 'cost_issue_status', 'CostAI问题处理状态'
) v
where not exists (select 1 from sys_dict_type t where t.dict_type = v.dict_type);

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
     status, create_by, create_time, remark)
select v.sort_no, v.label_name, v.value_code, v.dict_type, '', v.list_class,
       'N', '0', 'admin', sysdate(), v.remark
from (
    select 1 sort_no, '执行中' label_name, 'RUNNING' value_code, 'cost_review_status' dict_type, 'primary' list_class, '规则正在执行' remark union all
    select 2, '成功', 'SUCCESS', 'cost_review_status', 'success', '规则执行成功' union all
    select 3, '失败', 'FAILED', 'cost_review_status', 'danger', '规则执行失败' union all
    select 1, '数量为负', 'NEGATIVE_QUANTITY', 'cost_review_issue_type', 'danger', '工程量小于0' union all
    select 2, '单价为负', 'NEGATIVE_UNIT_PRICE', 'cost_review_issue_type', 'danger', '综合单价小于0' union all
    select 3, '零单价', 'ZERO_UNIT_PRICE', 'cost_review_issue_type', 'warning', '有工程量但单价为0' union all
    select 4, '合价计算异常', 'TOTAL_CALC_ERROR', 'cost_review_issue_type', 'warning', '工程量乘单价与Excel合价不一致' union all
    select 5, '重复清单', 'DUPLICATE_ITEM', 'cost_review_issue_type', 'warning', '同批次高度一致清单' union all
    select 6, '工程量差异', 'QUANTITY_DIFF', 'cost_review_issue_type', 'warning', '左右工程量差异超阈值' union all
    select 7, '综合单价差异', 'UNIT_PRICE_DIFF', 'cost_review_issue_type', 'warning', '左右单价差异超阈值' union all
    select 8, '合价差异', 'TOTAL_PRICE_DIFF', 'cost_review_issue_type', 'warning', '左右合价差异超阈值' union all
    select 9, '仅左侧存在', 'ONLY_LEFT', 'cost_review_issue_type', 'danger', '疑似删除项或右侧漏项' union all
    select 10, '仅右侧存在', 'ONLY_RIGHT', 'cost_review_issue_type', 'danger', '疑似新增项或左侧漏项' union all
    select 1, '提示', 'INFO', 'cost_issue_level', 'info', '提示级问题' union all
    select 2, '低风险', 'LOW', 'cost_issue_level', 'info', '低风险问题' union all
    select 3, '中风险', 'MEDIUM', 'cost_issue_level', 'warning', '中风险问题' union all
    select 4, '高风险', 'HIGH', 'cost_issue_level', 'danger', '高风险问题' union all
    select 5, '严重', 'CRITICAL', 'cost_issue_level', 'danger', '严重问题' union all
    select 1, '待确认', 'PENDING', 'cost_issue_status', 'warning', '等待人工确认' union all
    select 2, '已确认', 'CONFIRMED', 'cost_issue_status', 'danger', '人工确认问题' union all
    select 3, '已忽略', 'IGNORED', 'cost_issue_status', 'info', '人工忽略问题' union all
    select 4, '已整改', 'RECTIFIED', 'cost_issue_status', 'success', '问题已整改'
) v
where not exists (
    select 1 from sys_dict_data d
    where d.dict_type = v.dict_type and d.dict_value = v.value_code
);

set @cost_project_menu_id = (
    select menu_id from sys_menu where perms = 'cost:project:list' and menu_type = 'C' limit 1
);

insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select v.menu_name, @cost_project_menu_id, v.order_num, '', null, '', '', 1, 0, 'F', '0', '0',
       v.perms, '#', 'admin', sysdate(), v.remark
from (
    select '审核批次查询' menu_name, 31 order_num, 'cost:review:list' perms, '查询规则审核批次和问题' remark union all
    select '审核详情查询', 32, 'cost:review:query', '查看审核批次详情和汇总' union all
    select '执行规则审核', 33, 'cost:review:start', '执行纯Java确定性规则审核' union all
    select '审核规则配置', 34, 'cost:review:config', '查看并修改审核规则配置'
) v
where @cost_project_menu_id is not null
  and not exists (select 1 from sys_menu m where m.perms = v.perms);
