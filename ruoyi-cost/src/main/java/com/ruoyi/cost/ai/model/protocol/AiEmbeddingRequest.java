package com.ruoyi.cost.ai.model.protocol;

import java.util.List;

public class AiEmbeddingRequest
{
    private Long modelConfigId;
    private List<String> inputs;
    private AiInvocationContext context;

    public Long getModelConfigId() { return modelConfigId; }
    public void setModelConfigId(Long modelConfigId) { this.modelConfigId = modelConfigId; }
    public List<String> getInputs() { return inputs; }
    public void setInputs(List<String> inputs) { this.inputs = inputs; }
    public AiInvocationContext getContext() { return context; }
    public void setContext(AiInvocationContext context) { this.context = context; }
}
