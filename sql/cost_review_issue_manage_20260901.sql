-- CostAI 阶段07：审核任务语义升级与审核问题人工处理
-- 可重复执行；将阶段06 cost_review_batch 原位重命名，不复制第二套审核任务/问题数据。

set @schema_name = database();

set @has_old_task = (select count(*) from information_schema.tables
    where table_schema = @schema_name and table_name = 'cost_review_batch');
set @has_new_task = (select count(*) from information_schema.tables
    where table_schema = @schema_name and table_name = 'cost_review_task');
set @ddl = if(@has_old_task = 1 and @has_new_task = 0,
    'rename table cost_review_batch to cost_review_task', 'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

set @has_task_name = (select count(*) from information_schema.columns
    where table_schema = @schema_name and table_name = 'cost_review_task' and column_name = 'task_name');
set @ddl = if(@has_task_name = 0,
    'alter table cost_review_task add column task_name varchar(200) null comment ''任务名称'' after right_batch_id',
    'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;
update cost_review_task set task_name = concat('规则审核 #', id)
where task_name is null or task_name = '';
alter table cost_review_task modify column task_name varchar(200) not null comment '任务名称';

set @has_review_batch_id = (select count(*) from information_schema.columns
    where table_schema = @schema_name and table_name = 'cost_review_issue' and column_name = 'review_batch_id');
set @has_review_task_id = (select count(*) from information_schema.columns
    where table_schema = @schema_name and table_name = 'cost_review_issue' and column_name = 'review_task_id');
set @ddl = if(@has_review_batch_id = 1 and @has_review_task_id = 0,
    'alter table cost_review_issue change column review_batch_id review_task_id bigint not null comment ''审核任务ID''',
    'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

set @has_left_item_id = (select count(*) from information_schema.columns
    where table_schema = @schema_name and table_name = 'cost_review_issue' and column_name = 'left_item_id');
set @ddl = if(@has_left_item_id = 0,
    'alter table cost_review_issue add column left_item_id bigint null comment ''左侧清单ID'' after project_id',
    'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

set @has_right_item_id = (select count(*) from information_schema.columns
    where table_schema = @schema_name and table_name = 'cost_review_issue' and column_name = 'right_item_id');
set @ddl = if(@has_right_item_id = 0,
    'alter table cost_review_issue add column right_item_id bigint null comment ''右侧清单ID'' after left_item_id',
    'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

set @has_reviewer = (select count(*) from information_schema.columns
    where table_schema = @schema_name and table_name = 'cost_review_issue' and column_name = 'reviewer');
set @ddl = if(@has_reviewer = 0,
    'alter table cost_review_issue add column reviewer varchar(64) null comment ''处理人账号'' after reviewer_user_id',
    'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

update cost_review_issue i
left join cost_boq_compare c on c.id = i.compare_result_id
set i.left_item_id = coalesce(i.left_item_id,
        case when i.item_side = 'LEFT' then i.boq_item_id else c.left_item_id end),
    i.right_item_id = coalesce(i.right_item_id,
        case when i.item_side = 'RIGHT' then i.boq_item_id else c.right_item_id end),
    i.reviewer = coalesce(i.reviewer, nullif(i.update_by, ''));

update cost_review_issue
set issue_type = case rule_code
    when 'NEGATIVE_QUANTITY' then 'QUANTITY'
    when 'QUANTITY_DIFF' then 'QUANTITY'
    when 'NEGATIVE_UNIT_PRICE' then 'UNIT_PRICE'
    when 'ZERO_UNIT_PRICE' then 'UNIT_PRICE'
    when 'TOTAL_CALC_ERROR' then 'TOTAL_PRICE'
    when 'TOTAL_PRICE_DIFF' then 'TOTAL_PRICE'
    when 'DUPLICATE_ITEM' then 'DUPLICATE'
    when 'ONLY_LEFT' then 'MISSING'
    when 'ONLY_RIGHT' then 'NEW_ITEM'
    else issue_type end;

set @has_left_index = (select count(*) from information_schema.statistics
    where table_schema = @schema_name and table_name = 'cost_review_issue' and index_name = 'idx_review_issue_left');
set @ddl = if(@has_left_index = 0,
    'alter table cost_review_issue add index idx_review_issue_left (left_item_id)', 'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

set @has_right_index = (select count(*) from information_schema.statistics
    where table_schema = @schema_name and table_name = 'cost_review_issue' and index_name = 'idx_review_issue_right');
set @ddl = if(@has_right_index = 0,
    'alter table cost_review_issue add index idx_review_issue_right (right_item_id)', 'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

insert into sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
     status, create_by, create_time, remark)
select v.sort_no, v.label_name, v.value_code, 'cost_review_issue_type', '', v.list_class,
       'N', '0', 'admin', sysdate(), v.remark
from (
    select 1 sort_no, '工程量异常' label_name, 'QUANTITY' value_code, 'warning' list_class, '工程量问题大类' remark union all
    select 2, '综合单价异常', 'UNIT_PRICE', 'warning', '综合单价问题大类' union all
    select 3, '合价异常', 'TOTAL_PRICE', 'danger', '合价问题大类' union all
    select 4, '重复清单', 'DUPLICATE', 'warning', '重复问题大类' union all
    select 5, '疑似漏项', 'MISSING', 'danger', '仅左侧存在问题大类' union all
    select 6, '新增清单', 'NEW_ITEM', 'primary', '仅右侧存在问题大类' union all
    select 7, '项目特征异常', 'FEATURE', 'warning', '项目特征问题大类' union all
    select 8, '数据异常', 'DATA', 'danger', '数据质量问题大类' union all
    select 9, '其他', 'OTHER', 'info', '其他问题大类'
) v
where not exists (select 1 from sys_dict_data d
    where d.dict_type = 'cost_review_issue_type' and d.dict_value = v.value_code);

set @cost_project_menu_id = (
    select menu_id from sys_menu where perms = 'cost:project:list' and menu_type = 'C' limit 1
);
insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '审核问题处理', @cost_project_menu_id, 35, '', null, '', '', 1, 0,
       'F', '0', '0', 'cost:review:handle', '#', 'admin', sysdate(), '确认、忽略、整改审核问题'
where @cost_project_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'cost:review:handle');
