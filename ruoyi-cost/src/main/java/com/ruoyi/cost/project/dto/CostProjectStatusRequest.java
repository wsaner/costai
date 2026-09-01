package com.ruoyi.cost.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 项目状态修改请求。 */
public class CostProjectStatusRequest
{
    @NotNull(message = "项目ID不能为空")
    private Long id;

    @NotBlank(message = "项目状态不能为空")
    private String projectStatus;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getProjectStatus()
    {
        return projectStatus;
    }

    public void setProjectStatus(String projectStatus)
    {
        this.projectStatus = projectStatus;
    }
}
