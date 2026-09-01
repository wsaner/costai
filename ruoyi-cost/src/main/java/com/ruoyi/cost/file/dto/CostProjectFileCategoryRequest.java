package com.ruoyi.cost.file.dto;

import jakarta.validation.constraints.NotBlank;

/** 修改项目文件分类请求。 */
public class CostProjectFileCategoryRequest
{
    private String fileCategory;

    @NotBlank(message = "文件分类不能为空")
    public String getFileCategory()
    {
        return fileCategory;
    }

    public void setFileCategory(String fileCategory)
    {
        this.fileCategory = fileCategory;
    }
}
