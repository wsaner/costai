-- CostAI 阶段04：工程量清单批次、清单明细、错误行、字典与权限
-- 可重复执行；不覆盖已有字典和菜单配置。

create table if not exists cost_boq_batch (
    id                  bigint          not null auto_increment comment '导入批次ID',
    project_id          bigint          not null comment '项目ID',
    batch_name          varchar(100)    not null comment '批次名称',
    business_type       varchar(32)     not null comment '业务类型',
    source_file_id      bigint          not null comment '项目文件记录ID',
    sheet_name          varchar(100)    not null comment '来源Sheet',
    header_row          int             not null comment '表头行号（1基）',
    field_mapping_json  json            not null comment '用户确认字段映射',
    professional_type   varchar(32)     default null comment '专业类型',
    total_count         int             not null default 0 comment '有效数据总行数',
    success_count       int             not null default 0 comment '成功条数',
    fail_count          int             not null default 0 comment '失败条数',
    total_amount        decimal(24,6)   not null default 0 comment '成功行原始合价汇总',
    import_status       varchar(32)     not null comment '导入状态',
    error_summary       varchar(1000)   default null comment '致命错误摘要',
    create_by           varchar(64)     default '' comment '创建者',
    create_time         datetime        not null default current_timestamp comment '创建时间',
    update_by           varchar(64)     default '' comment '更新者',
    update_time         datetime        default null comment '更新时间',
    remark              varchar(500)    default null comment '备注',
    del_flag            char(1)         not null default '0' comment '删除标志（0存在 2删除）',
    primary key (id),
    key idx_boq_batch_project (project_id, create_time),
    key idx_boq_batch_source_file (source_file_id),
    key idx_boq_batch_status (project_id, import_status)
) engine=InnoDB comment='工程量清单导入批次';

create table if not exists cost_boq_item (
    id                      bigint          not null auto_increment comment '清单ID',
    project_id              bigint          not null comment '项目ID',
    batch_id                bigint          not null comment '导入批次ID',
    sequence_no             varchar(64)     default null comment '序号',
    item_code               varchar(100)    default null comment '项目编码',
    item_name               varchar(500)    not null comment '项目名称',
    item_feature            varchar(2000)   default null comment '项目特征',
    unit                    varchar(50)     default null comment '单位',
    quantity                decimal(24,8)   default null comment '工程量',
    unit_price              decimal(24,8)   default null comment '综合单价',
    total_price             decimal(24,6)   default null comment 'Excel原始合价',
    calculated_total_price  decimal(24,6)   default null comment '工程量乘综合单价计算值',
    labor_price             decimal(24,6)   default null comment '人工费',
    material_price          decimal(24,6)   default null comment '材料费',
    machine_price           decimal(24,6)   default null comment '机械费',
    management_fee          decimal(24,6)   default null comment '管理费',
    profit                  decimal(24,6)   default null comment '利润',
    tax                     decimal(24,6)   default null comment '税金',
    professional_type       varchar(32)     default null comment '专业类型',
    category                varchar(100)    default null comment '清单分类',
    parent_id               bigint          default null comment '父级清单ID',
    item_level              int             default null comment '层级',
    source_sheet            varchar(100)    not null comment '来源Sheet',
    source_row              int             not null comment '来源行号（1基）',
    create_by               varchar(64)     default '' comment '创建者',
    create_time             datetime        not null default current_timestamp comment '创建时间',
    update_by               varchar(64)     default '' comment '更新者',
    update_time             datetime        default null comment '更新时间',
    remark                  varchar(500)    default null comment '备注',
    del_flag                char(1)         not null default '0' comment '删除标志（0存在 2删除）',
    primary key (id),
    key idx_boq_item_project (project_id),
    key idx_boq_item_batch (batch_id, source_row),
    key idx_boq_item_code (project_id, item_code),
    key idx_boq_item_name (project_id, item_name(100)),
    key idx_boq_item_professional (project_id, professional_type)
) engine=InnoDB comment='工程量清单明细';

create table if not exists cost_boq_import_error (
    id              bigint          not null auto_increment comment '错误ID',
    project_id      bigint          not null comment '项目ID',
    batch_id        bigint          not null comment '导入批次ID',
    source_sheet    varchar(100)    not null comment '来源Sheet',
    source_row      int             not null comment '来源行号（1基）',
    error_field     varchar(64)     default null comment '错误标准字段',
    raw_value       varchar(1000)   default null comment '原始错误值',
    error_message   varchar(1000)   not null comment '错误说明',
    raw_data_json   longtext        default null comment '原始行JSON',
    create_by       varchar(64)     default '' comment '创建者',
    create_time     datetime        not null default current_timestamp comment '创建时间',
    update_by       varchar(64)     default '' comment '更新者',
    update_time     datetime        default null comment '更新时间',
    remark          varchar(500)    default null comment '备注',
    del_flag        char(1)         not null default '0' comment '删除标志（0存在 2删除）',
    primary key (id),
    key idx_boq_error_batch (batch_id, source_row),
    key idx_boq_error_project (project_id)
) engine=InnoDB comment='工程量清单导入错误行';

insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '清单业务类型', 'cost_boq_business_type', '0', 'admin', sysdate(), 'CostAI清单导入业务类型'
where not exists (select 1 from sys_dict_type where dict_type = 'cost_boq_business_type');

insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '清单导入状态', 'cost_boq_import_status', '0', 'admin', sysdate(), 'CostAI清单导入状态'
where not exists (select 1 from sys_dict_type where dict_type = 'cost_boq_import_status');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
select v.sort_no, v.label_name, v.value_code, 'cost_boq_business_type', '', 'default', 'N', '0', 'admin', sysdate()
from (
    select 1 sort_no, '工程量清单' label_name, 'BOQ' value_code union all
    select 2, '控制价', 'CONTROL_PRICE' union all
    select 3, '投标报价', 'BID_PRICE' union all
    select 4, '送审预算', 'SUBMITTED' union all
    select 5, '审核预算', 'REVIEWED' union all
    select 6, '结算书', 'SETTLEMENT' union all
    select 7, '其他', 'OTHER'
) v
where not exists (
    select 1 from sys_dict_data d where d.dict_type = 'cost_boq_business_type' and d.dict_value = v.value_code
);

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
select v.sort_no, v.label_name, v.value_code, 'cost_boq_import_status', '', v.list_class, 'N', '0', 'admin', sysdate()
from (
    select 1 sort_no, '导入中' label_name, 'IMPORTING' value_code, 'primary' list_class union all
    select 2, '成功', 'SUCCESS', 'success' union all
    select 3, '部分失败', 'PARTIAL_FAILED', 'warning' union all
    select 4, '失败', 'FAILED', 'danger'
) v
where not exists (
    select 1 from sys_dict_data d where d.dict_type = 'cost_boq_import_status' and d.dict_value = v.value_code
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
    select '清单导入' menu_name, 22 order_num, 'cost:boq:import' perms, '正式导入工程量清单' remark union all
    select '清单列表', 23, 'cost:boq:list', '查询清单批次和明细' union all
    select '清单详情', 24, 'cost:boq:query', '查询清单与错误详情' union all
    select '删除清单批次', 25, 'cost:boq:remove', '级联删除清单批次数据'
) v
where @cost_project_menu_id is not null
  and not exists (select 1 from sys_menu m where m.perms = v.perms);
