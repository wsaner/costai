package com.ruoyi.cost.ai.chat.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AiConversationCreateRequest
{
    @Size(max = 100, message = "会话标题不能超过100个字符")
    private String title;
    @Pattern(regexp = "GENERAL|PROJECT", message = "会话模式无效")
    private String mode;
    private Long projectId;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
}
