package com.ruoyi.cost.ai.chat.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.core.domain.BaseEntity;

/** AI造价助手会话消息。 */
public class AiConversationMessage extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long conversationId;
    private Long userId;
    private String role;
    private String content;
    private String status;
    private String model;
    private String requestId;
    private String sourcesJson;
    private String toolCallsJson;
    private String tokenUsageJson;
    private String errorMessage;
    @JsonIgnore
    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getSourcesJson() { return sourcesJson; }
    public void setSourcesJson(String sourcesJson) { this.sourcesJson = sourcesJson; }
    public String getToolCallsJson() { return toolCallsJson; }
    public void setToolCallsJson(String toolCallsJson) { this.toolCallsJson = toolCallsJson; }
    public String getTokenUsageJson() { return tokenUsageJson; }
    public void setTokenUsageJson(String tokenUsageJson) { this.tokenUsageJson = tokenUsageJson; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
