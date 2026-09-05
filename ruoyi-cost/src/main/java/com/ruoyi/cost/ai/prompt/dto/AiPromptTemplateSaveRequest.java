package com.ruoyi.cost.ai.prompt.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AiPromptTemplateSaveRequest
{
    private Long id;
    @NotBlank(message = "Prompt编码不能为空")
    @Pattern(regexp = "[A-Za-z][A-Za-z0-9_.-]{0,63}", message = "Prompt编码格式不正确")
    private String promptCode;
    @NotBlank(message = "Prompt名称不能为空") @Size(max = 100, message = "Prompt名称不能超过100个字符")
    private String promptName;
    @NotBlank(message = "System Prompt不能为空") @Size(max = 50000, message = "System Prompt不能超过50000个字符")
    private String systemPrompt;
    @NotBlank(message = "用户模板不能为空") @Size(max = 50000, message = "用户模板不能超过50000个字符")
    private String userTemplate;
    @NotNull(message = "版本号不能为空") @Min(value = 1, message = "版本号必须大于0")
    private Integer version;
    @Pattern(regexp = "[01]", message = "启用状态不正确")
    private String enabled;
    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPromptCode() { return promptCode; }
    public void setPromptCode(String promptCode) { this.promptCode = promptCode; }
    public String getPromptName() { return promptName; }
    public void setPromptName(String promptName) { this.promptName = promptName; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public String getUserTemplate() { return userTemplate; }
    public void setUserTemplate(String userTemplate) { this.userTemplate = userTemplate; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
