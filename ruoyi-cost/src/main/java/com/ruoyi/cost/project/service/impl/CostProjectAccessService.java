package com.ruoyi.cost.project.service.impl;

import org.springframework.stereotype.Service;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.cost.project.domain.CostProject;
import com.ruoyi.cost.project.mapper.CostProjectMapper;

/**
 * 项目访问范围查询。单独成 Bean，确保修改、删除等内部校验也经过数据权限 AOP。
 */
@Service
public class CostProjectAccessService
{
    private final CostProjectMapper projectMapper;

    public CostProjectAccessService(CostProjectMapper projectMapper)
    {
        this.projectMapper = projectMapper;
    }

    @DataScope(deptAlias = "p", userAlias = "p", deptField = "owner_dept_id", userField = "project_manager_id")
    public CostProject selectAccessibleProject(CostProject query)
    {
        return projectMapper.selectCostProjectById(query);
    }
}
