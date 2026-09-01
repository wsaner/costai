-- CostAI 阶段02：项目文件中心
-- 适用：MySQL 8.x / RuoYi 3.9.2。依赖 cost_project_20260901.sql。

create table if not exists cost_project_file
(
    id                  bigint          not null auto_increment comment '项目文件主键',
    project_id          bigint          not null comment '造价项目ID',
    file_id             varchar(64)     not null comment '稳定文件业务标识',
    original_name       varchar(255)    not null comment '原始文件名',
    file_name           varchar(255)    not null comment '存储文件名',
    file_ext            varchar(16)     not null comment '文件扩展名（小写）',
    mime_type           varchar(128)    not null comment 'MIME类型',
    file_size           bigint unsigned not null comment '文件大小（字节）',
    storage_path        varchar(500)    not null comment 'profile目录下的私有相对存储路径',
    file_category       varchar(32)     not null comment '文件分类（cost_file_category）',
    file_hash           char(64)        not null comment '文件SHA-256摘要',
    ai_parse_status     varchar(20)     not null default 'WAITING' comment 'AI解析状态（cost_ai_parse_status）',
    ai_parse_text       longtext        default null comment 'AI解析文本',
    ai_parse_error      varchar(2000)   default null comment 'AI解析错误信息',
    create_by           varchar(64)     default '' comment '创建者',
    create_time         datetime        default null comment '创建时间',
    update_by           varchar(64)     default '' comment '更新者',
    update_time         datetime        default null comment '更新时间',
    remark              varchar(500)    default null comment '备注',
    del_flag            char(1)         not null default '0' comment '删除标志（0存在 2删除）',
    primary key (id),
    unique key uk_cost_project_file_file_id (file_id),
    key idx_cost_project_file_project (project_id),
    key idx_cost_project_file_category (project_id, file_category),
    key idx_cost_project_file_parse (project_id, ai_parse_status),
    key idx_cost_project_file_create_time (create_time)
) engine=innodb comment='造价项目文件表';

insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '项目文件分类', 'cost_file_category', '0', 'admin', sysdate(), '造价项目文件分类'
where not exists (select 1 from sys_dict_type where dict_type = 'cost_file_category');
insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select 'AI文件解析状态', 'cost_ai_parse_status', '0', 'admin', sysdate(), 'AI文件解析状态'
where not exists (select 1 from sys_dict_type where dict_type = 'cost_ai_parse_status');

drop procedure if exists cost_add_project_file_dict;
delimiter $$
create procedure cost_add_project_file_dict(
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

call cost_add_project_file_dict(1,  '招标文件', 'TENDER_DOCUMENT',         'cost_file_category', '', 'Y');
call cost_add_project_file_dict(2,  '投标文件', 'BID_DOCUMENT',            'cost_file_category', '', 'N');
call cost_add_project_file_dict(3,  '施工合同', 'CONSTRUCTION_CONTRACT',    'cost_file_category', '', 'N');
call cost_add_project_file_dict(4,  '补充协议', 'SUPPLEMENTARY_AGREEMENT',  'cost_file_category', '', 'N');
call cost_add_project_file_dict(5,  '设计图纸', 'DESIGN_DRAWING',           'cost_file_category', '', 'N');
call cost_add_project_file_dict(6,  '工程量清单', 'BOQ',                    'cost_file_category', '', 'N');
call cost_add_project_file_dict(7,  '预算文件', 'BUDGET_DOCUMENT',          'cost_file_category', '', 'N');
call cost_add_project_file_dict(8,  '结算文件', 'SETTLEMENT_DOCUMENT',      'cost_file_category', '', 'N');
call cost_add_project_file_dict(9,  '变更文件', 'CHANGE_DOCUMENT',          'cost_file_category', '', 'N');
call cost_add_project_file_dict(10, '签证文件', 'VISA_DOCUMENT',            'cost_file_category', '', 'N');
call cost_add_project_file_dict(11, '询价文件', 'INQUIRY_DOCUMENT',         'cost_file_category', '', 'N');
call cost_add_project_file_dict(12, '审核资料', 'AUDIT_MATERIAL',           'cost_file_category', '', 'N');
call cost_add_project_file_dict(13, '其他',     'OTHER',                    'cost_file_category', '', 'N');

call cost_add_project_file_dict(1, '等待解析', 'WAITING',     'cost_ai_parse_status', 'info',    'Y');
call cost_add_project_file_dict(2, '解析中',   'PARSING',     'cost_ai_parse_status', 'primary', 'N');
call cost_add_project_file_dict(3, '解析成功', 'SUCCESS',     'cost_ai_parse_status', 'success', 'N');
call cost_add_project_file_dict(4, '解析失败', 'FAILED',      'cost_ai_parse_status', 'danger',  'N');
call cost_add_project_file_dict(5, '暂不支持', 'UNSUPPORTED', 'cost_ai_parse_status', 'info',    'N');

drop procedure if exists cost_add_project_file_dict;

set @cost_project_menu_id = (
    select menu_id from sys_menu where perms = 'cost:project:list' and menu_type = 'C' limit 1
);

insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '项目文件列表', @cost_project_menu_id, 11, '', null, '', '', 1, 0, 'F', '0', '0',
       'cost:file:list', '#', 'admin', sysdate(), '项目文件列表权限'
where @cost_project_menu_id is not null and not exists (select 1 from sys_menu where perms = 'cost:file:list');
insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '项目文件查询', @cost_project_menu_id, 12, '', null, '', '', 1, 0, 'F', '0', '0',
       'cost:file:query', '#', 'admin', sysdate(), '项目文件查询权限'
where @cost_project_menu_id is not null and not exists (select 1 from sys_menu where perms = 'cost:file:query');
insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '项目文件上传', @cost_project_menu_id, 13, '', null, '', '', 1, 0, 'F', '0', '0',
       'cost:file:upload', '#', 'admin', sysdate(), '项目文件上传权限'
where @cost_project_menu_id is not null and not exists (select 1 from sys_menu where perms = 'cost:file:upload');
insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '项目文件分类', @cost_project_menu_id, 14, '', null, '', '', 1, 0, 'F', '0', '0',
       'cost:file:edit', '#', 'admin', sysdate(), '项目文件分类修改权限'
where @cost_project_menu_id is not null and not exists (select 1 from sys_menu where perms = 'cost:file:edit');
insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '项目文件删除', @cost_project_menu_id, 15, '', null, '', '', 1, 0, 'F', '0', '0',
       'cost:file:remove', '#', 'admin', sysdate(), '项目文件删除权限'
where @cost_project_menu_id is not null and not exists (select 1 from sys_menu where perms = 'cost:file:remove');
insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '项目文件下载', @cost_project_menu_id, 16, '', null, '', '', 1, 0, 'F', '0', '0',
       'cost:file:download', '#', 'admin', sysdate(), '项目文件下载权限'
where @cost_project_menu_id is not null and not exists (select 1 from sys_menu where perms = 'cost:file:download');
