-- CostAI 阶段03：工程量清单Excel解析与字段识别权限
-- 本阶段不创建清单主表；正式导入批次与明细表留待阶段04。

set @cost_project_menu_id = (
    select menu_id from sys_menu where perms = 'cost:project:list' and menu_type = 'C' limit 1
);

insert into sys_menu
    (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '清单Excel预览', @cost_project_menu_id, 21, '', null, '', '', 1, 0, 'F', '0', '0',
       'cost:boq:preview', '#', 'admin', sysdate(), '工程量清单Excel解析与字段识别权限'
where @cost_project_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'cost:boq:preview');
