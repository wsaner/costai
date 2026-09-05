-- CostAI 阶段05：工程量清单对比结果、匹配状态字典与权限
-- 可重复执行；不覆盖已有字典和菜单配置。

create table if not exists cost_boq_compare (
    id                    bigint          not null auto_increment comment '对比结果ID',
    project_id            bigint          not null comment '项目ID',
    left_batch_id         bigint          not null comment '左侧清单批次ID',
    right_batch_id        bigint          not null comment '右侧清单批次ID',
    left_item_id          bigint          default null comment '左侧清单ID，ONLY_RIGHT时为空',
    right_item_id         bigint          default null comment '右侧清单ID，ONLY_LEFT时为空',
    match_type            varchar(32)     not null comment '匹配类型',
    match_score           decimal(8,6)    not null default 0 comment '匹配度0到1',
    quantity_diff         decimal(24,8)   default null comment '工程量差异（左减右）',
    quantity_diff_rate    decimal(18,6)   default null comment '工程量差异率（差异除以左值绝对值）',
    unit_price_diff       decimal(24,8)   default null comment '综合单价差异（左减右）',
    unit_price_diff_rate  decimal(18,6)   default null comment '综合单价差异率',
    total_price_diff      decimal(24,6)   default null comment '合价差异（左减右）',
    total_price_diff_rate decimal(18,6)   default null comment '合价差异率',
    create_by             varchar(64)     default '' comment '创建者',
    create_time           datetime        not null default current_timestamp comment '创建时间',
    update_by             varchar(64)     default '' comment '更新者',
    update_time           datetime        default null comment '更新时间',
    remark                varchar(500)    default null comment '备注',
    del_flag              char(1)         not null default '0' comment '删除标志（0存在 2删除）',
    primary key (id),
    key idx_boq_compare_pair (project_id, left_batch_id, right_batch_id, match_type),
    key idx_boq_compare_left_item (left_item_id),
    key idx_boq_compare_right_item (right_item_id),
    key idx_boq_compare_left_batch (left_batch_id),
    key idx_boq_compare_right_batch (right_batch_id)
) engine=InnoDB comment='工程量清单对比结果';

insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '清单匹配状态', 'cost_boq_match_type', '0', 'admin', sysdate(), 'CostAI清单对比匹配状态'
where not exists (select 1 from sys_dict_type where dict_type = 'cost_boq_match_type');

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
     status, create_by, create_time, remark)
select v.sort_no, v.label_name, v.value_code, 'cost_boq_match_type', '', v.list_class,
       'N', '0', 'admin', sysdate(), v.remark
from (
    select 1 sort_no, '精确匹配' label_name, 'EXACT' value_code, 'success' list_class, '编码或标准化名称与单位完全一致' remark union all
    select 2, '高相似度', 'HIGH_SIMILARITY', 'primary', '名称、特征和单位高相似' union all
    select 3, '低相似度', 'LOW_SIMILARITY', 'warning', '名称、特征和单位低相似，建议人工确认' union all
    select 4, '仅左侧', 'ONLY_LEFT', 'danger', '仅左侧批次存在' union all
    select 5, '仅右侧', 'ONLY_RIGHT', 'danger', '仅右侧批次存在' union all
    select 6, '人工匹配', 'MANUAL', 'info', '由用户人工指定的优先匹配'
) v
where not exists (
    select 1 from sys_dict_data d
    where d.dict_type = 'cost_boq_match_type' and d.dict_value = v.value_code
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
    select '清单对比查询' menu_name, 26 order_num, 'cost:compare:list' perms, '查询清单对比结果' remark union all
    select '执行清单匹配', 27, 'cost:compare:start', '首次匹配与重新匹配' union all
    select '人工调整匹配', 28, 'cost:compare:manual', '人工指定或取消清单匹配'
) v
where @cost_project_menu_id is not null
  and not exists (select 1 from sys_menu m where m.perms = v.perms);
