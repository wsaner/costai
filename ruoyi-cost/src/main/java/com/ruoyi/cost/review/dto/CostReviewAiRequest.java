package com.ruoyi.cost.review.dto;

import jakarta.validation.constraints.Size;

/** 单问题 AI 语义分析请求。 */
public class CostReviewAiRequest
{
    private Long modelConfigId;
    @Size(max = 2000, message = "补充上下文不能超过2000个字符")
    private String additionalContext;

    public Long getModelConfigId() { return modelConfigId; }
    public void setModelConfigId(Long modelConfigId) { this.modelConfigId = modelConfigId; }
    public String getAdditionalContext() { return additionalContext; }
    public void setAdditionalContext(String additionalContext) { this.additionalContext = additionalContext; }
}
