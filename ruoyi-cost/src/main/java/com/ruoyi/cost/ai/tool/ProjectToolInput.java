package com.ruoyi.cost.ai.tool;

import com.ruoyi.cost.project.domain.CostProject;

/** project 由上层现有项目Service完成数据权限校验后传入。 */
public record ProjectToolInput(Long projectId, String keyword, int limit, CostProject project) { }
