package com.ruoyi.cost.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 修改单个审核规则配置值。 */
public class RuleConfigUpdateRequest
{
    @NotBlank(message = "配置值不能为空")
    @Size(max = 1000, message = "配置值不能超过1000个字符")
    private String configValue;

    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
}
