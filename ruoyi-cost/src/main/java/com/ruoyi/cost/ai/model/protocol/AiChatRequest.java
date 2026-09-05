package com.ruoyi.cost.ai.model.protocol;

import java.math.BigDecimal;
import java.util.List;

/** 普通/流式对话统一请求。modelConfigId 为空时选择已启用的默认配置。 */
public class AiChatRequest
{
    private Long modelConfigId;
    private List<AiMessage> messages;
    private BigDecimal temperature;
    private Integer maxTokens;
    private AiInvocationContext context;

    public Long getModelConfigId() { return modelConfigId; }
    public void setModelConfigId(Long modelConfigId) { this.modelConfigId = modelConfigId; }
    public List<AiMessage> getMessages() { return messages; }
    public void setMessages(List<AiMessage> messages) { this.messages = messages; }
    public BigDecimal getTemperature() { return temperature; }
    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public AiInvocationContext getContext() { return context; }
    public void setContext(AiInvocationContext context) { this.context = context; }
}
