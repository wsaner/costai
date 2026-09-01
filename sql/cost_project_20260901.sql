-- CostAI 阶段01：造价项目管理
-- 适用：MySQL 8.x / RuoYi 3.9.2。先执行原始 RuoYi 初始化脚本，再执行本脚本。

create table if not exists cost_project
(
    id                       bigint          not null auto_increment comment '项目主键',
    project_code             varchar(64)     not null comment '项目编号',
    project_name             varchar(200)    not null comment '项目名称',
    project_type             varchar(32)     not null comment '项目类型（cost_project_type）',
    professional_type        varchar(32)     not null comment '项目专业（cost_professional_type）',
    project_stage            varchar(32)     not null comment '项目阶段（cost_project_stage）',
    construction_unit        varchar(200)    default null comment '建设单位',
    contractor_unit          varchar(200)    default null comment '施工单位',
    consulting_unit          varchar(200)    default null comment '咨询单位',
    province                 varchar(64)     default null comment '省',
    city                     varchar(64)     default null comment '市',
    district                 varchar(64)     default null comment '区县',
    building_area            decimal(20,4)   default null comment '建筑面积（平方米）',
    project_manager_id       bigint          not null comment '项目负责人用户ID',
    project_manager_name     varchar(100)    not null comment '项目负责人名称快照',
    owner_dept_id            bigint          not null comment '归属部门ID（数据权限）',
    submitted_amount         decimal(20,2)   not null default 0.00 comment '送审金额',
    approved_amount          decimal(20,2)   not null default 0.00 comment '审定金额',
    increase_amount          decimal(20,2)   not null default 0.00 comment '核增金额',
    reduction_amount         decimal(20,2)   not null default 0.00 comment '核减金额',
    reduction_rate           decimal(12,6)   not null default 0.000000 comment '核减率（小数）',
    start_date               date            default null comment '开工日期',
    completion_date          date            default null comment '竣工日期',
    project_status           varchar(32)     not null comment '项目状态（cost_project_status）',
    description              varchar(2000)   default null comment '项目描述',
    create_by                varchar(64)     default '' comment '创建者',
    create_time              datetime        default null comment '创建时间',
    update_by                varchar(64)     default '' comment '更新者',
    update_time              datetime        default null comment '更新时间',
    remark                   varchar(500)    default null comment '备注',
    del_flag                 char(1)         not null default '0' comment '删除标志（0存在 2删除）',
    primary key (id),
    unique key uk_cost_project_code (project_code),
    key idx_cost_project_name (project_name),
    key idx_cost_project_type (project_type),
    key idx_cost_project_stage (project_stage),
    key idx_cost_project_status (project_status),
    key idx_cost_project_manager (project_manager_id),
    key idx_cost_project_dept (owner_dept_id),
    key idx_cost_project_create_time (create_time)
) engine=innodb comment='造价项目表';

-- 字典类型
insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '造价项目类型', 'cost_project_type', '0', 'admin', sysdate(), '造价项目类型'
where not exists (select 1 from sys_dict_type where dict_type = 'cost_project_type');
insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '造价项目阶段', 'cost_project_stage', '0', 'admin', sysdate(), '造价项目阶段'
where not exists (select 1 from sys_dict_type where dict_type = 'cost_project_stage');
insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '造价项目状态', 'cost_project_status', '0', 'admin', sysdate(), '造价项目状态'
where not exists (select 1 from sys_dict_type where dict_type = 'cost_project_status');
insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '造价项目专业', 'cost_professional_type', '0', 'admin', sysdate(), '造价项目专业'
where not exists (select 1 from sys_dict_type where dict_type = 'cost_professional_type');

-- 字典数据辅助过程：按 dict_type + dict_value 保证重复执行安全。
drop procedure if exists cost_add_dict_data;
delimiter $$
create procedure cost_add_dict_data(
    in p_sort int, in p_label varchar(100), in p_value varchar(100),
    in p_type varchar(100), in p_list_class varchar(100), in p_default char(1))
begin
    if not exists (select 1 from sys_dict_data where dict_type = p_type and dict_value = p_value) then
        insert into sys_dict_data
            (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
             status, create_by, create_time, remark)
        values
            (p_sort, p_label, p_value, p_type, '', p_list_class, p_default,
             '0', 'admin', sysdate(), concat(p_label, '字典项'));
    end if;
end$$
delimiter ;

call cost_add_dict_data(1,  '住宅', 'RESIDENTIAL',       'cost_project_type', '', 'Y');
call cost_add_dict_data(2,  '商业', 'COMMERCIAL',        'cost_project_type', '', 'N');
call cost_add_dict_data(3,  '办公', 'OFFICE',            'cost_project_type', '', 'N');
call cost_add_dict_data(4,  '工业', 'INDUSTRIAL',        'cost_project_type', '', 'N');
call cost_add_dict_data(5,  '医院', 'HOSPITAL',          'cost_project_type', '', 'N');
call cost_add_dict_data(6,  '学校', 'SCHOOL',            'cost_project_type', '', 'N');
call cost_add_dict_data(7,  '市政', 'MUNICIPAL',         'cost_project_type', '', 'N');
call cost_add_dict_data(8,  '公路', 'HIGHWAY',           'cost_project_type', '', 'N');
call cost_add_dict_data(9,  '水利', 'WATER_CONSERVANCY', 'cost_project_type', '', 'N');
call cost_add_dict_data(10, '园林', 'LANDSCAPE',         'cost_project_type', '', 'N');
call cost_add_dict_data(11, '装修', 'DECORATION',        'cost_project_type', '', 'N');
call cost_add_dict_data(12, '安装', 'INSTALLATION',      'cost_project_type', '', 'N');
call cost_add_dict_data(13, '其他', 'OTHER',             'cost_project_type', '', 'N');

call cost_add_dict_data(1, '投资估算',   'ESTIMATE',            'cost_project_stage', '', 'Y');
call cost_add_dict_data(2, '设计概算',   'DESIGN_BUDGET',       'cost_project_stage', '', 'N');
call cost_add_dict_data(3, '施工图预算', 'CONSTRUCTION_BUDGET', 'cost_project_stage', '', 'N');
call cost_add_dict_data(4, '招标控制价', 'TENDER_CONTROL',      'cost_project_stage', '', 'N');
call cost_add_dict_data(5, '投标报价',   'BID_QUOTATION',       'cost_project_stage', '', 'N');
call cost_add_dict_data(6, '施工过程',   'CONSTRUCTION',        'cost_project_stage', '', 'N');
call cost_add_dict_data(7, '结算审核',   'SETTLEMENT_AUDIT',    'cost_project_stage', '', 'N');
call cost_add_dict_data(8, '竣工决算',   'FINAL_ACCOUNT',       'cost_project_stage', '', 'N');

call cost_add_dict_data(1, '准备中', 'PREPARING',    'cost_project_status', 'info',    'Y');
call cost_add_dict_data(2, '进行中', 'IN_PROGRESS',  'cost_project_status', 'primary', 'N');
call cost_add_dict_data(3, '审核中', 'UNDER_REVIEW', 'cost_project_status', 'warning', 'N');
call cost_add_dict_data(4, '已完成', 'COMPLETED',    'cost_project_status', 'success', 'N');
call cost_add_dict_data(5, '已归档', 'ARCHIVED',     'cost_project_status', 'info',    'N');

call cost_add_dict_data(1, '综合', 'COMPREHENSIVE', 'cost_professional_type', '', 'Y');
call cost_add_dict_data(2, '土建', 'CIVIL',         'cost_professional_type', '', 'N');
call cost_add_dict_data(3, '安装', 'INSTALLATION',  'cost_professional_type', '', 'N');
call cost_add_dict_data(4, '市政', 'MUNICIPAL',     'cost_professional_type', '', 'N');
call cost_add_dict_data(5, '园林', 'LANDSCAPE',     'cost_professional_type', '', 'N');
call cost_add_dict_data(6, '装修', 'DECORATION',    'cost_professional_type', '', 'N');
call cost_add_dict_data(7, '其他', 'OTHER',         'cost_professional_type', '', 'N');

drop procedure if exists cost_add_dict_data;

-- 菜单与按钮权限。非管理员角色需在“角色管理”中按职责授权。
insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '造价管理', 0, 5, 'cost', null, '', '', 1, 0, 'M', '0', '0', '', 'money',
       'admin', sysdate(), '造价管理目录'
where not exists (select 1 from sys_menu where parent_id = 0 and path = 'cost');

set @cost_root_menu_id = (select menu_id from sys_menu where parent_id = 0 and path = 'cost' limit 1);

insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '项目管理', @cost_root_menu_id, 1, 'project', 'cost/project/index', '', 'CostProject',
       1, 0, 'C', '0', '0', 'cost:project:list', 'project', 'admin', sysdate(), '造价项目管理菜单'
where not exists (select 1 from sys_menu where parent_id = @cost_root_menu_id and path = 'project');

set @cost_project_menu_id = (
    select menu_id from sys_menu where parent_id = @cost_root_menu_id and path = 'project' limit 1
);

insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '项目查询', @cost_project_menu_id, 1, '', null, '', '', 1, 0, 'F', '0', '0',
       'cost:project:query', '#', 'admin', sysdate(), '造价项目查询按钮'
where not exists (select 1 from sys_menu where perms = 'cost:project:query');
insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '项目新增', @cost_project_menu_id, 2, '', null, '', '', 1, 0, 'F', '0', '0',
       'cost:project:add', '#', 'admin', sysdate(), '造价项目新增按钮'
where not exists (select 1 from sys_menu where perms = 'cost:project:add');
insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '项目修改', @cost_project_menu_id, 3, '', null, '', '', 1, 0, 'F', '0', '0',
       'cost:project:edit', '#', 'admin', sysdate(), '造价项目修改按钮'
where not exists (select 1 from sys_menu where perms = 'cost:project:edit');
insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '项目删除', @cost_project_menu_id, 4, '', null, '', '', 1, 0, 'F', '0', '0',
       'cost:project:remove', '#', 'admin', sysdate(), '造价项目删除按钮'
where not exists (select 1 from sys_menu where perms = 'cost:project:remove');
